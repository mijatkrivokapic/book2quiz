import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GenerationRecordService } from '../../../core/services/generation-record.service';
import { NotificationService } from '../../../core/services/notification.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';
import {
  GenerationKind,
  GenerationRecord,
  GenerationUsageSummary,
  TypeUsage
} from '../../../core/models/generation-record.model';

const KIND_LABELS: Record<GenerationKind, string> = {
  CHARACTERISTICS: 'Characteristics',
  QUESTIONS: 'Questions',
  QUESTION_REGENERATION: 'Question regeneration'
};

@Component({
  selector: 'app-generation-history',
  imports: [DatePipe, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatTooltipModule],
  standalone: true,
  templateUrl: './generation-history.component.html',
  styleUrl: './generation-history.component.css'
})
export class GenerationHistoryComponent implements OnInit {
  private readonly service = inject(GenerationRecordService);
  private readonly notification = inject(NotificationService);

  readonly bookId = input.required<number>();
  readonly ordinal = input.required<number>();

  protected readonly records = signal<GenerationRecord[]>([]);
  protected readonly summary = signal<GenerationUsageSummary | null>(null);
  protected readonly loading = signal(true);

  // Per-type rollup as a template-friendly array, in a stable order.
  protected readonly byTypeEntries = computed<{ kind: GenerationKind; label: string; usage: TypeUsage }[]>(() => {
    const summary = this.summary();
    if (!summary) {
      return [];
    }
    const order: GenerationKind[] = ['CHARACTERISTICS', 'QUESTIONS', 'QUESTION_REGENERATION'];
    return order
      .filter(kind => summary.byType[kind])
      .map(kind => ({ kind, label: KIND_LABELS[kind], usage: summary.byType[kind]! }));
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.service.list(this.bookId(), this.ordinal()).subscribe({
      next: records => {
        this.records.set(records);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.notification.error(extractErrorMessage(err, 'Failed to load the generation history.'));
      }
    });
    this.service.summary(this.bookId(), this.ordinal()).subscribe({
      next: summary => this.summary.set(summary),
      error: () => {} // summary is secondary; the table still renders
    });
  }

  protected kindLabel(kind: GenerationKind): string {
    return KIND_LABELS[kind];
  }

  protected kindClass(kind: GenerationKind): string {
    return 'kind-' + kind.toLowerCase();
  }

  /** Formats a token count with thousands separators. */
  protected tokens(value: number): string {
    return value.toLocaleString('en-US');
  }

  /** Formats a millisecond duration as "820 ms", "3.4 s" or "2m 05s". */
  protected duration(ms: number): string {
    if (ms < 1000) {
      return `${ms} ms`;
    }
    const seconds = ms / 1000;
    if (seconds < 60) {
      return `${seconds.toFixed(1)} s`;
    }
    const mins = Math.floor(seconds / 60);
    const rem = Math.round(seconds % 60);
    return `${mins}m ${rem.toString().padStart(2, '0')}s`;
  }
}
