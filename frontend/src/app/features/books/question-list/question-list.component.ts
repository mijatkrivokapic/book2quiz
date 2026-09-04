import { Component, OnDestroy, OnInit, computed, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { QuestionService } from '../../../core/services/question.service';
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

const POLL_INTERVAL_MS = 3000;

@Component({
  selector: 'app-question-list',
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatTooltipModule],
  standalone: true,
  templateUrl: './question-list.component.html',
  styleUrl: './question-list.component.css'
})
export class QuestionListComponent implements OnInit, OnDestroy {
  private readonly questionService = inject(QuestionService);
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

  private pollHandle?: ReturnType<typeof setTimeout>;
  private regenPollHandle?: ReturnType<typeof setTimeout>;

  ngOnInit(): void {
    this.load();
    // Resume the progress indicator if a generation is still running (e.g. after a reload).
    this.questionService.getGenerationStatus(this.bookId(), this.ordinal()).subscribe({
      next: s => {
        if (s.status === 'PROCESSING') {
          this.generating.set(true);
          this.schedulePoll();
        }
      },
      error: () => {}
    });
  }

  ngOnDestroy(): void {
    clearTimeout(this.pollHandle);
    clearTimeout(this.regenPollHandle);
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
            // Optimistically mark this card as processing, then poll.
            this.questions.update(list =>
              list.map(q => (q.id === question.id ? { ...q, regenerationStatus: 'PROCESSING' } : q))
            );
            this.scheduleRegenPoll();
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

  private scheduleRegenPoll(): void {
    clearTimeout(this.regenPollHandle);
    if (this.anyRegenerating()) {
      this.regenPollHandle = setTimeout(() => this.pollRegen(), POLL_INTERVAL_MS);
    }
  }

  private pollRegen(): void {
    this.questionService.list(this.bookId(), this.ordinal()).subscribe({
      next: questions => {
        const wasRegenerating = this.anyRegenerating();
        this.questions.set(questions);
        if (this.anyRegenerating()) {
          this.scheduleRegenPoll();
        } else if (wasRegenerating) {
          this.notification.success('Question regenerated (new version pending review).');
        }
      },
      error: () => this.scheduleRegenPoll()
    });
  }

  protected generate(): void {
    this.generating.set(true);
    this.questionService.generate(this.bookId(), this.ordinal()).subscribe({
      next: () => {
        this.notification.success('Question generation started.');
        this.schedulePoll();
      },
      error: err => {
        this.generating.set(false);
        this.notification.error(extractErrorMessage(err, 'Failed to start question generation.'));
      }
    });
  }

  private schedulePoll(): void {
    clearTimeout(this.pollHandle);
    this.pollHandle = setTimeout(() => this.pollStatus(), POLL_INTERVAL_MS);
  }

  private pollStatus(): void {
    this.questionService.getGenerationStatus(this.bookId(), this.ordinal()).subscribe({
      next: s => {
        if (s.status === 'PROCESSING') {
          this.schedulePoll();
        } else if (s.status === 'DONE') {
          this.generating.set(false);
          this.notification.success('Questions generated (pending review).');
          this.load();
        } else if (s.status === 'FAILED') {
          this.generating.set(false);
          this.notification.error(s.error ?? 'Question generation failed.');
        } else {
          this.generating.set(false);
        }
      },
      error: () => this.schedulePoll()
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
        // Resume the regeneration indicator if a run is still in progress.
        this.scheduleRegenPoll();
      },
      error: err => {
        this.loading.set(false);
        this.notification.error(extractErrorMessage(err, 'Failed to load questions.'));
      }
    });
  }
}
