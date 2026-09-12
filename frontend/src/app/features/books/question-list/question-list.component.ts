import { Component, OnDestroy, OnInit, computed, inject, input, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { QuestionService } from '../../../core/services/question.service';
import { GenerationEventsService } from '../../../core/services/generation-events.service';
import { NotificationService } from '../../../core/services/notification.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';
import { renderMarkdown, renderMarkdownInline } from '../../../core/utils/markdown.util';
import { Question, QuestionPayload, QuestionStatus } from '../../../core/models/question.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import {
  QuestionEditorDialogComponent,
  QuestionEditorDialogData
} from '../question-editor-dialog/question-editor-dialog.component';
import { RegenerateQuestionDialogComponent } from '../regenerate-question-dialog/regenerate-question-dialog.component';
import {
  QuestionVersionsDialogComponent,
  QuestionVersionsDialogData
} from '../question-versions-dialog/question-versions-dialog.component';

@Component({
  selector: 'app-question-list',
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatTooltipModule],
  standalone: true,
  templateUrl: './question-list.component.html',
  styleUrl: './question-list.component.css'
})
export class QuestionListComponent implements OnInit, OnDestroy {
  private readonly questionService = inject(QuestionService);
  private readonly events = inject(GenerationEventsService);
  private readonly notification = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  readonly bookId = input.required<number>();
  readonly ordinal = input.required<number>();

  // Markdown renderers for the template ([innerHTML] output is sanitized by Angular).
  protected readonly render = (md: string) => renderMarkdown(md);
  protected readonly renderInline = (md: string) => renderMarkdownInline(md);

  protected readonly questions = signal<Question[]>([]);
  protected readonly loading = signal(true);
  protected readonly generating = signal(false);
  protected readonly busyId = signal<number | null>(null);

  protected readonly approvedCount = computed(
    () => this.questions().filter(q => q.status === 'APPROVED').length
  );

  private eventsSub?: Subscription;

  ngOnInit(): void {
    this.load();
    this.resumeGenerationStatus();

    this.eventsSub = this.events.stream(this.bookId()).subscribe(msg => {
      if (msg.type === 'connected') {
        this.resumeGenerationStatus();       // reconcile question generation
        if (this.anyRegenerating()) {
          this.load();                        // reconcile in-flight regenerations
        }
        return;
      }
      const event = msg.event;
      if (event.ordinal !== this.ordinal()) {
        return;
      }
      if (event.kind === 'QUESTIONS') {
        if (event.status === 'DONE') {
          this.generating.set(false);
          this.notification.success('Questions generated (pending review).');
          this.load();
        } else if (event.status === 'FAILED') {
          this.generating.set(false);
          this.notification.error(event.error ?? 'Question generation failed.');
        }
      } else if (event.kind === 'REGENERATION') {
        if (event.status === 'DONE') {
          this.notification.success('Question regenerated (new version pending review).');
        }
        this.load(); // refresh regenerationStatus / content (DONE or FAILED)
      }
    });
  }

  ngOnDestroy(): void {
    this.eventsSub?.unsubscribe();
  }

  protected regenerate(question: Question): void {
    this.dialog
      .open(RegenerateQuestionDialogComponent, { width: '560px' })
      .afterClosed()
      .subscribe((guideline: string | null) => {
        if (guideline === null) {
          return;
        }
        this.questionService.regenerate(this.bookId(), this.ordinal(), question.id, guideline).subscribe({
          next: () => {
            this.notification.success('Regeneration started.');
            // Optimistically mark this card as processing; completion arrives via SSE.
            this.questions.update(list =>
              list.map(q => (q.id === question.id ? { ...q, regenerationStatus: 'PROCESSING' } : q))
            );
          },
          error: err => this.notification.error(extractErrorMessage(err, 'Failed to start regeneration.'))
        });
      });
  }

  protected openVersions(question: Question): void {
    const data: QuestionVersionsDialogData = {
      bookId: this.bookId(),
      ordinal: this.ordinal(),
      questionId: question.id
    };
    this.dialog
      .open(QuestionVersionsDialogComponent, { width: '760px', data })
      .afterClosed()
      .subscribe((changed: boolean) => {
        if (changed) {
          this.load();
        }
      });
  }

  private anyRegenerating(): boolean {
    return this.questions().some(q => q.regenerationStatus === 'PROCESSING');
  }

  protected generate(): void {
    this.generating.set(true);
    this.questionService.generate(this.bookId(), this.ordinal()).subscribe({
      next: () => this.notification.success('Question generation started.'),
      error: err => {
        this.generating.set(false);
        this.notification.error(extractErrorMessage(err, 'Failed to start question generation.'));
      }
    });
  }

  /** One-shot status check (on init and on SSE reconnect) to resume/reconcile generation. */
  private resumeGenerationStatus(): void {
    this.questionService.getGenerationStatus(this.bookId(), this.ordinal()).subscribe({
      next: s => {
        if (s.status === 'PROCESSING') {
          this.generating.set(true);
        } else if (s.status === 'DONE' && this.generating()) {
          this.generating.set(false);
          this.load();
        } else if (s.status === 'FAILED' && this.generating()) {
          this.generating.set(false);
          this.notification.error(s.error ?? 'Question generation failed.');
        }
      },
      error: () => {}
    });
  }

  protected add(): void {
    const data: QuestionEditorDialogData = {};
    this.dialog
      .open(QuestionEditorDialogComponent, { width: '760px', data })
      .afterClosed()
      .subscribe((payload: QuestionPayload | null) => {
        if (!payload) {
          return;
        }
        this.questionService.create(this.bookId(), this.ordinal(), payload).subscribe({
          next: created => {
            this.questions.update(list => [...list, created]);
            this.notification.success('Question added.');
          },
          error: err => this.notification.error(extractErrorMessage(err, 'Failed to add the question.'))
        });
      });
  }

  protected edit(question: Question): void {
    const data: QuestionEditorDialogData = { question: question.question };
    this.dialog
      .open(QuestionEditorDialogComponent, { width: '760px', data })
      .afterClosed()
      .subscribe((payload: QuestionPayload | null) => {
        if (!payload) {
          return;
        }
        this.questionService.update(this.bookId(), this.ordinal(), question.id, payload).subscribe({
          next: updated => {
            this.questions.update(list => list.map(q => (q.id === updated.id ? updated : q)));
            this.notification.success('Question updated.');
          },
          error: err => this.notification.error(extractErrorMessage(err, 'Failed to update the question.'))
        });
      });
  }

  protected setStatus(question: Question, status: QuestionStatus): void {
    this.busyId.set(question.id);
    this.questionService.updateStatus(this.bookId(), this.ordinal(), question.id, status).subscribe({
      next: updated => {
        this.busyId.set(null);
        this.questions.update(list => list.map(q => (q.id === updated.id ? updated : q)));
      },
      error: err => {
        this.busyId.set(null);
        this.notification.error(extractErrorMessage(err, 'Failed to update the status.'));
      }
    });
  }

  protected remove(question: Question): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '420px',
      data: {
        title: 'Delete question',
        message: 'Delete this question? This cannot be undone.',
        confirmLabel: 'Delete'
      }
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) {
        return;
      }
      this.questionService.delete(this.bookId(), this.ordinal(), question.id).subscribe({
        next: () => {
          this.questions.update(list => list.filter(q => q.id !== question.id));
          this.notification.success('Question deleted.');
        },
        error: err => this.notification.error(extractErrorMessage(err, 'Failed to delete the question.'))
      });
    });
  }

  /** Copies a single question's JSON payload to the clipboard. */
  protected exportQuestion(question: Question): void {
    this.copyJson(question.question, 'Question JSON copied to clipboard.');
  }

  /** Copies a JSON array of all APPROVED questions' payloads to the clipboard. */
  protected exportApproved(): void {
    const approved = this.questions()
      .filter(q => q.status === 'APPROVED')
      .map(q => q.question);
    if (approved.length === 0) {
      this.notification.error('No approved questions to export.');
      return;
    }
    this.copyJson(approved, `Copied ${approved.length} approved question(s) to clipboard.`);
  }

  private copyJson(data: unknown, successMessage: string): void {
    const json = JSON.stringify(data, null, 2);
    navigator.clipboard.writeText(json).then(
      () => this.notification.success(successMessage),
      () => this.notification.error('Could not copy to clipboard.')
    );
  }

  private load(): void {
    this.loading.set(true);
    this.questionService.list(this.bookId(), this.ordinal()).subscribe({
      next: questions => {
        this.questions.set(questions);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.notification.error(extractErrorMessage(err, 'Failed to load questions.'));
      }
    });
  }
}
