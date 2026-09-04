import { Component, OnInit, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { QuestionService } from '../../../core/services/question.service';
import { NotificationService } from '../../../core/services/notification.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';
import { renderMarkdown } from '../../../core/utils/markdown.util';
import { QuestionPayload, QuestionVersion } from '../../../core/models/question.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import {
  QuestionEditorDialogComponent,
  QuestionEditorDialogData
} from '../question-editor-dialog/question-editor-dialog.component';

export interface QuestionVersionsDialogData {
  bookId: number;
  ordinal: number;
  questionId: number;
}

@Component({
  selector: 'app-question-versions-dialog',
  imports: [
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule
  ],
  standalone: true,
  templateUrl: './question-versions-dialog.component.html',
  styleUrl: './question-versions-dialog.component.css'
})
export class QuestionVersionsDialogComponent implements OnInit {
  private readonly dialogRef = inject(MatDialogRef<QuestionVersionsDialogComponent>);
  private readonly data = inject<QuestionVersionsDialogData>(MAT_DIALOG_DATA);
  private readonly questionService = inject(QuestionService);
  private readonly notification = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  // Markdown renderer for the template ([innerHTML] output is sanitized by Angular).
  protected readonly render = (md: string) => renderMarkdown(md);

  protected readonly versions = signal<QuestionVersion[]>([]);
  protected readonly loading = signal(true);
  protected readonly busyId = signal<number | null>(null);
  private changed = false;

  ngOnInit(): void {
    this.load();
  }

  protected activate(version: QuestionVersion): void {
    if (version.active) {
      return;
    }
    this.busyId.set(version.id);
    this.questionService.activateVersion(this.data.bookId, this.data.ordinal, this.data.questionId, version.id)
      .subscribe({
        next: () => {
          this.busyId.set(null);
          this.changed = true;
          this.versions.update(list => list.map(v => ({ ...v, active: v.id === version.id })));
          this.notification.success('Active version updated.');
        },
        error: err => {
          this.busyId.set(null);
          this.notification.error(extractErrorMessage(err, 'Failed to activate the version.'));
        }
      });
  }

  protected edit(version: QuestionVersion): void {
    const data: QuestionEditorDialogData = { question: version.question };
    this.dialog
      .open(QuestionEditorDialogComponent, { width: '760px', data })
      .afterClosed()
      .subscribe((payload: QuestionPayload | null) => {
        if (!payload) {
          return;
        }
        this.questionService
          .updateVersion(this.data.bookId, this.data.ordinal, this.data.questionId, version.id, payload)
          .subscribe({
            next: updated => {
              this.changed = true;
              this.versions.update(list => list.map(v => (v.id === updated.id ? updated : v)));
              this.notification.success('Version updated.');
            },
            error: err => this.notification.error(extractErrorMessage(err, 'Failed to update the version.'))
          });
      });
  }

  protected remove(version: QuestionVersion): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '420px',
      data: {
        title: 'Delete version',
        message: 'Delete this version? This cannot be undone.',
        confirmLabel: 'Delete'
      }
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) {
        return;
      }
      this.busyId.set(version.id);
      this.questionService.deleteVersion(this.data.bookId, this.data.ordinal, this.data.questionId, version.id)
        .subscribe({
          next: () => {
            this.busyId.set(null);
            this.changed = true;
            this.versions.update(list => list.filter(v => v.id !== version.id));
            this.notification.success('Version deleted.');
          },
          error: err => {
            this.busyId.set(null);
            this.notification.error(extractErrorMessage(err, 'Failed to delete the version.'));
          }
        });
    });
  }

  protected close(): void {
    this.dialogRef.close(this.changed);
  }

  private load(): void {
    this.loading.set(true);
    this.questionService.listVersions(this.data.bookId, this.data.ordinal, this.data.questionId).subscribe({
      next: versions => {
        this.versions.set(versions);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.notification.error(extractErrorMessage(err, 'Failed to load versions.'));
      }
    });
  }
}
