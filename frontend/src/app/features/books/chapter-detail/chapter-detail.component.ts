import { Component, OnDestroy, OnInit, computed, inject, signal, viewChildren } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog } from '@angular/material/dialog';
import { BookService } from '../../../core/services/book.service';
import { ChapterService } from '../../../core/services/chapter.service';
import { CharacteristicService } from '../../../core/services/characteristic.service';
import { LearningObjectiveService } from '../../../core/services/learning-objective.service';
import { ConstraintService } from '../../../core/services/constraint.service';
import { NotificationService } from '../../../core/services/notification.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';
import { renderMarkdown } from '../../../core/utils/markdown.util';
import { Book } from '../../../core/models/book.model';
import { Chapter } from '../../../core/models/chapter.model';
import { Characteristic, CharacteristicStatus, LearningObjective } from '../../../core/models/characteristic.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { EditableItemListComponent } from '../editable-item-list/editable-item-list.component';
import { QuestionListComponent } from '../question-list/question-list.component';

type DetailView = 'content' | 'characteristics' | 'questions';

/** CRUD closures for one learning objective's structural characteristics list. */
interface StructuralAdapters {
  load: () => Observable<Characteristic[]>;
  create: (content: string) => Observable<Characteristic>;
  update: (id: number, content: string) => Observable<Characteristic>;
  remove: (id: number) => Observable<void>;
  updateStatus: (id: number, status: CharacteristicStatus) => Observable<Characteristic>;
}

const CHAR_POLL_INTERVAL_MS = 3000;

@Component({
  selector: 'app-chapter-detail',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
    MatButtonToggleModule,
    EditableItemListComponent,
    QuestionListComponent
  ],
  standalone: true,
  templateUrl: './chapter-detail.component.html',
  styleUrl: './chapter-detail.component.css'
})
export class ChapterDetailComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  private readonly bookService = inject(BookService);
  private readonly chapterService = inject(ChapterService);
  private readonly characteristicService = inject(CharacteristicService);
  private readonly learningObjectiveService = inject(LearningObjectiveService);
  private readonly constraintService = inject(ConstraintService);
  private readonly notification = inject(NotificationService);

  protected readonly bookId = Number(this.route.snapshot.paramMap.get('bookId'));
  protected readonly ordinal = Number(this.route.snapshot.paramMap.get('ordinal'));

  protected readonly book = signal<Book | null>(null);
  protected readonly chapter = signal<Chapter | null>(null);
  protected readonly markdown = signal<string>('');
  protected readonly editContent = signal<string>('');

  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);
  protected readonly editing = signal(false);
  protected readonly saving = signal(false);
  protected readonly view = signal<DetailView>('content');

  // Surface characteristics: CRUD adapters handed to the generic editable-item-list.
  protected readonly loadSurface = () =>
    this.characteristicService.list(this.bookId, this.ordinal);
  protected readonly createSurface = (content: string) =>
    this.characteristicService.create(this.bookId, this.ordinal, content);
  protected readonly updateSurface = (id: number, content: string) =>
    this.characteristicService.update(this.bookId, this.ordinal, id, content);
  protected readonly removeSurface = (id: number) =>
    this.characteristicService.delete(this.bookId, this.ordinal, id);
  protected readonly updateSurfaceStatus = (id: number, status: CharacteristicStatus) =>
    this.characteristicService.updateStatus(this.bookId, this.ordinal, id, status);

  // Learning objectives, each owning a nested structural-characteristics list.
  protected readonly learningObjectives = signal<LearningObjective[]>([]);
  protected readonly loadingObjectives = signal(true);
  protected readonly newObjective = signal('');
  protected readonly addingObjective = signal(false);
  protected readonly editingObjectiveId = signal<number | null>(null);
  protected readonly editObjectiveDescription = signal('');
  protected readonly busyObjectiveId = signal<number | null>(null);

  // Stable per-objective structural adapters, so the child lists keep the same function
  // identities across change-detection cycles.
  private readonly structuralAdaptersCache = new Map<number, StructuralAdapters>();

  protected structuralAdapters(loId: number): StructuralAdapters {
    let adapters = this.structuralAdaptersCache.get(loId);
    if (!adapters) {
      adapters = {
        load: () => this.learningObjectiveService.listStructural(this.bookId, this.ordinal, loId),
        create: (content: string) =>
          this.learningObjectiveService.createStructural(this.bookId, this.ordinal, loId, content),
        update: (id: number, content: string) =>
          this.learningObjectiveService.updateStructural(this.bookId, this.ordinal, loId, id, content),
        remove: (id: number) =>
          this.learningObjectiveService.deleteStructural(this.bookId, this.ordinal, loId, id),
        updateStatus: (id: number, status: CharacteristicStatus) =>
          this.learningObjectiveService.updateStructuralStatus(this.bookId, this.ordinal, loId, id, status)
      };
      this.structuralAdaptersCache.set(loId, adapters);
    }
    return adapters;
  }

  // Async characteristic generation.
  protected readonly generatingCharacteristics = signal(false);
  private readonly characteristicLists = viewChildren(EditableItemListComponent);
  private charPollHandle?: ReturnType<typeof setTimeout>;

  protected readonly loadConstraints = () => this.constraintService.list(this.bookId, this.ordinal);
  protected readonly createConstraint = (content: string) =>
    this.constraintService.create(this.bookId, this.ordinal, content);
  protected readonly updateConstraint = (id: number, content: string) =>
    this.constraintService.update(this.bookId, this.ordinal, id, content);
  protected readonly removeConstraint = (id: number) =>
    this.constraintService.delete(this.bookId, this.ordinal, id);

  protected readonly editingTitle = signal(false);
  protected readonly savingTitle = signal(false);
  protected readonly titleForm = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]]
  });

  protected readonly renderedView = computed(() => renderMarkdown(this.markdown()));
  protected readonly renderedPreview = computed(() => renderMarkdown(this.editContent()));

  ngOnInit(): void {
    this.bookService.getById(this.bookId).subscribe({
      next: book => this.book.set(book),
      error: () => {} // non-fatal: the book title is only contextual
    });

    this.chapterService.getChapter(this.bookId, this.ordinal).subscribe({
      next: chapter => {
        this.chapter.set(chapter);
        this.loadMarkdown(chapter);
      },
      error: err => {
        this.loading.set(false);
        this.notFound.set(true);
        this.notification.error(extractErrorMessage(err, 'Failed to load the chapter.'));
      }
    });

    this.loadObjectives();

    // Resume the characteristic-generation indicator if a run is still in progress.
    this.characteristicService.getGenerationStatus(this.bookId, this.ordinal).subscribe({
      next: s => {
        if (s.status === 'PROCESSING') {
          this.generatingCharacteristics.set(true);
          this.scheduleCharPoll();
        }
      },
      error: () => {}
    });
  }

  ngOnDestroy(): void {
    clearTimeout(this.charPollHandle);
  }

  protected generateCharacteristics(): void {
    this.generatingCharacteristics.set(true);
    this.characteristicService.generate(this.bookId, this.ordinal).subscribe({
      next: () => {
        this.notification.success('Characteristic generation started.');
        this.scheduleCharPoll();
      },
      error: err => {
        this.generatingCharacteristics.set(false);
        this.notification.error(extractErrorMessage(err, 'Failed to start characteristic generation.'));
      }
    });
  }

  private scheduleCharPoll(): void {
    clearTimeout(this.charPollHandle);
    this.charPollHandle = setTimeout(() => this.pollCharStatus(), CHAR_POLL_INTERVAL_MS);
  }

  private pollCharStatus(): void {
    this.characteristicService.getGenerationStatus(this.bookId, this.ordinal).subscribe({
      next: s => {
        if (s.status === 'PROCESSING') {
          this.scheduleCharPoll();
        } else if (s.status === 'DONE') {
          this.generatingCharacteristics.set(false);
          this.notification.success('Characteristics generated (pending review).');
          this.characteristicLists().forEach(list => list.reload());
        } else if (s.status === 'FAILED') {
          this.generatingCharacteristics.set(false);
          this.notification.error(s.error ?? 'Characteristic generation failed.');
        } else {
          this.generatingCharacteristics.set(false);
        }
      },
      error: () => this.scheduleCharPoll()
    });
  }

  // ---- Learning objectives -------------------------------------------------------------

  private loadObjectives(): void {
    this.loadingObjectives.set(true);
    this.learningObjectiveService.list(this.bookId, this.ordinal).subscribe({
      next: objectives => {
        this.learningObjectives.set(objectives);
        this.loadingObjectives.set(false);
      },
      error: err => {
        this.loadingObjectives.set(false);
        this.notification.error(extractErrorMessage(err, 'Failed to load learning objectives.'));
      }
    });
  }

  protected onNewObjectiveInput(event: Event): void {
    this.newObjective.set((event.target as HTMLTextAreaElement).value);
  }

  protected onEditObjectiveInput(event: Event): void {
    this.editObjectiveDescription.set((event.target as HTMLTextAreaElement).value);
  }

  protected addObjective(): void {
    const description = this.newObjective().trim();
    if (!description) {
      return;
    }
    this.addingObjective.set(true);
    this.learningObjectiveService.create(this.bookId, this.ordinal, description).subscribe({
      next: created => {
        this.learningObjectives.update(list => [...list, created]);
        this.newObjective.set('');
        this.addingObjective.set(false);
      },
      error: err => {
        this.addingObjective.set(false);
        this.notification.error(extractErrorMessage(err, 'Failed to add the learning objective.'));
      }
    });
  }

  protected startEditObjective(objective: LearningObjective): void {
    this.editingObjectiveId.set(objective.id);
    this.editObjectiveDescription.set(objective.description);
  }

  protected cancelEditObjective(): void {
    this.editingObjectiveId.set(null);
  }

  protected saveObjective(objective: LearningObjective): void {
    const description = this.editObjectiveDescription().trim();
    if (!description) {
      return;
    }
    this.busyObjectiveId.set(objective.id);
    this.learningObjectiveService.update(this.bookId, this.ordinal, objective.id, description).subscribe({
      next: updated => {
        this.learningObjectives.update(list => list.map(o => (o.id === updated.id ? updated : o)));
        this.busyObjectiveId.set(null);
        this.editingObjectiveId.set(null);
      },
      error: err => {
        this.busyObjectiveId.set(null);
        this.notification.error(extractErrorMessage(err, 'Failed to update the learning objective.'));
      }
    });
  }

  protected removeObjective(objective: LearningObjective): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '420px',
      data: {
        title: 'Delete learning objective',
        message: 'Delete this learning objective and all its structural characteristics? This cannot be undone.',
        confirmLabel: 'Delete'
      }
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) {
        return;
      }
      this.busyObjectiveId.set(objective.id);
      this.learningObjectiveService.delete(this.bookId, this.ordinal, objective.id).subscribe({
        next: () => {
          this.learningObjectives.update(list => list.filter(o => o.id !== objective.id));
          this.structuralAdaptersCache.delete(objective.id);
          this.busyObjectiveId.set(null);
        },
        error: err => {
          this.busyObjectiveId.set(null);
          this.notification.error(extractErrorMessage(err, 'Failed to delete the learning objective.'));
        }
      });
    });
  }

  protected startEdit(): void {
    this.editContent.set(this.markdown());
    this.editing.set(true);
  }

  protected cancelEdit(): void {
    this.editing.set(false);
  }

  protected onInput(event: Event): void {
    this.editContent.set((event.target as HTMLTextAreaElement).value);
  }

  protected save(): void {
    this.saving.set(true);
    this.chapterService.updateMarkdown(this.bookId, this.ordinal, this.editContent()).subscribe({
      next: updated => {
        this.chapter.set(updated);
        this.markdown.set(this.editContent());
        this.saving.set(false);
        this.editing.set(false);
        this.notification.success('Markdown saved.');
      },
      error: err => {
        this.saving.set(false);
        this.notification.error(extractErrorMessage(err, 'Failed to save the Markdown.'));
      }
    });
  }

  protected setView(view: DetailView): void {
    this.view.set(view);
  }

  protected startEditTitle(): void {
    const chapter = this.chapter();
    if (!chapter) {
      return;
    }
    this.titleForm.setValue({ title: chapter.title });
    this.editingTitle.set(true);
  }

  protected cancelEditTitle(): void {
    this.editingTitle.set(false);
  }

  protected saveTitle(): void {
    if (this.titleForm.invalid) {
      this.titleForm.markAllAsTouched();
      return;
    }
    this.savingTitle.set(true);
    this.chapterService.updateTitle(this.bookId, this.ordinal, this.titleForm.getRawValue().title).subscribe({
      next: updated => {
        this.chapter.set(updated);
        this.savingTitle.set(false);
        this.editingTitle.set(false);
        this.notification.success('Chapter renamed.');
      },
      error: err => {
        this.savingTitle.set(false);
        this.notification.error(extractErrorMessage(err, 'Failed to rename the chapter.'));
      }
    });
  }

  protected deleteChapter(): void {
    const chapter = this.chapter();
    if (!chapter) {
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '420px',
      data: {
        title: 'Delete chapter',
        message: `Delete chapter "${chapter.title}"? Its PDF and Markdown will be removed. This cannot be undone.`,
        confirmLabel: 'Delete'
      }
    });

    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) {
        return;
      }
      this.chapterService.deleteChapter(this.bookId, this.ordinal).subscribe({
        next: () => {
          this.notification.success(`Chapter "${chapter.title}" deleted.`);
          this.router.navigate(['/books', this.bookId, 'chapters']);
        },
        error: err => this.notification.error(extractErrorMessage(err, 'Failed to delete the chapter.'))
      });
    });
  }

  protected pdfUrl(): string {
    return this.chapterService.chapterPdfUrl(this.bookId, this.ordinal);
  }

  private loadMarkdown(chapter: Chapter): void {
    if (!chapter.markdownAvailable) {
      this.markdown.set('');
      this.loading.set(false);
      return;
    }
    this.chapterService.getMarkdown(this.bookId, this.ordinal).subscribe({
      next: text => {
        this.markdown.set(text);
        this.loading.set(false);
      },
      error: err => {
        this.markdown.set('');
        this.loading.set(false);
        this.notification.error(extractErrorMessage(err, 'Failed to load the Markdown.'));
      }
    });
  }
}
