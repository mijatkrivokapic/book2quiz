import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
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
import { ConstraintService } from '../../../core/services/constraint.service';
import { NotificationService } from '../../../core/services/notification.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';
import { renderMarkdown } from '../../../core/utils/markdown.util';
import { Book } from '../../../core/models/book.model';
import { Chapter } from '../../../core/models/chapter.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { EditableItemListComponent } from '../editable-item-list/editable-item-list.component';

type DetailView = 'content' | 'characteristics';

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
    EditableItemListComponent
  ],
  standalone: true,
  templateUrl: './chapter-detail.component.html',
  styleUrl: './chapter-detail.component.css'
})
export class ChapterDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  private readonly bookService = inject(BookService);
  private readonly chapterService = inject(ChapterService);
  private readonly characteristicService = inject(CharacteristicService);
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

  // CRUD adapters handed to the generic editable-item-list component.
  protected readonly loadStructural = () =>
    this.characteristicService.list(this.bookId, this.ordinal, 'structural');
  protected readonly createStructural = (content: string) =>
    this.characteristicService.create(this.bookId, this.ordinal, 'structural', content);
  protected readonly updateStructural = (id: number, content: string) =>
    this.characteristicService.update(this.bookId, this.ordinal, 'structural', id, content);
  protected readonly removeStructural = (id: number) =>
    this.characteristicService.delete(this.bookId, this.ordinal, 'structural', id);

  protected readonly loadSurface = () =>
    this.characteristicService.list(this.bookId, this.ordinal, 'surface');
  protected readonly createSurface = (content: string) =>
    this.characteristicService.create(this.bookId, this.ordinal, 'surface', content);
  protected readonly updateSurface = (id: number, content: string) =>
    this.characteristicService.update(this.bookId, this.ordinal, 'surface', id, content);
  protected readonly removeSurface = (id: number) =>
    this.characteristicService.delete(this.bookId, this.ordinal, 'surface', id);

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
