import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { BookService } from '../../../core/services/book.service';
import { ChapterService } from '../../../core/services/chapter.service';
import { NotificationService } from '../../../core/services/notification.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';
import { Book } from '../../../core/models/book.model';
import { Chapter } from '../../../core/models/chapter.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import {
  AddChapterDialogComponent,
  AddChapterDialogData
} from '../add-chapter-dialog/add-chapter-dialog.component';

const POLL_INTERVAL_MS = 4000;
const MAX_EMPTY_POLLS = 15;

@Component({
  selector: 'app-book-chapters',
  imports: [
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  standalone: true,
  templateUrl: './book-chapters.component.html',
  styleUrl: './book-chapters.component.css'
})
export class BookChaptersComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly bookService = inject(BookService);
  private readonly chapterService = inject(ChapterService);
  private readonly notification = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  protected readonly bookId = Number(this.route.snapshot.paramMap.get('bookId'));
  protected readonly book = signal<Book | null>(null);
  protected readonly chapters = signal<Chapter[]>([]);
  protected readonly loading = signal(true);
  protected readonly extracting = signal(false);
  protected readonly retryingOrdinal = signal<number | null>(null);

  protected readonly displayedColumns = ['ordinal', 'title', 'pages', 'status', 'actions'];

  protected readonly inProgress = computed(() =>
    this.chapters().some(c => c.status === 'PENDING' || c.status === 'PROCESSING')
  );

  private pollHandle?: ReturnType<typeof setTimeout>;
  private emptyPolls = 0;

  ngOnInit(): void {
    this.loadBook();
    this.loadChapters(true);
  }

  ngOnDestroy(): void {
    clearTimeout(this.pollHandle);
  }

  protected extract(): void {
    if (this.chapters().length > 0) {
      const ref = this.dialog.open(ConfirmDialogComponent, {
        width: '420px',
        data: {
          title: 'Re-extract chapters',
          message: 'This discards the current chapters and their Markdown, then extracts again. Continue?',
          confirmLabel: 'Re-extract'
        }
      });
      ref.afterClosed().subscribe(confirmed => {
        if (confirmed) {
          this.startExtraction();
        }
      });
    } else {
      this.startExtraction();
    }
  }

  protected openAddChapter(): void {
    const data: AddChapterDialogData = { bookId: this.bookId };
    this.dialog
      .open(AddChapterDialogComponent, { width: '640px', data })
      .afterClosed()
      .subscribe((created: Chapter | null) => {
        if (!created) {
          return;
        }
        this.notification.success(`Chapter "${created.title}" added.`);
        this.loadChapters(false);
      });
  }

  protected retry(chapter: Chapter): void {
    this.retryingOrdinal.set(chapter.ordinal);
    this.chapterService.retryChapter(this.bookId, chapter.ordinal).subscribe({
      next: () => {
        this.retryingOrdinal.set(null);
        this.notification.success(`Retrying chapter ${chapter.ordinal}.`);
        this.loadChapters(false);
      },
      error: err => {
        this.retryingOrdinal.set(null);
        this.notification.error(extractErrorMessage(err, 'Failed to retry the chapter.'));
      }
    });
  }

  protected pdfUrl(chapter: Chapter): string {
    return this.chapterService.chapterPdfUrl(this.bookId, chapter.ordinal);
  }

  private startExtraction(): void {
    this.extracting.set(true);
    this.emptyPolls = 0;
    this.chapterService.extractChapters(this.bookId).subscribe({
      next: () => {
        this.notification.success('Chapter extraction started.');
        clearTimeout(this.pollHandle);
        this.pollHandle = setTimeout(() => this.loadChapters(false), POLL_INTERVAL_MS);
      },
      error: err => {
        this.extracting.set(false);
        this.notification.error(extractErrorMessage(err, 'Failed to start chapter extraction.'));
      }
    });
  }

  private loadBook(): void {
    this.bookService.getById(this.bookId).subscribe({
      next: book => this.book.set(book),
      error: err => this.notification.error(extractErrorMessage(err, 'Failed to load the book.'))
    });
  }

  private loadChapters(showSpinner: boolean): void {
    if (showSpinner) {
      this.loading.set(true);
    }
    this.chapterService.getChapters(this.bookId).subscribe({
      next: chapters => {
        this.chapters.set(chapters);
        this.loading.set(false);

        if (chapters.length > 0) {
          this.extracting.set(false);
        } else if (this.extracting()) {
          this.emptyPolls++;
          if (this.emptyPolls >= MAX_EMPTY_POLLS) {
            this.extracting.set(false);
            this.notification.error('Extraction did not produce any chapters. Please check the server logs.');
          }
        }
        this.scheduleNextPoll();
      },
      error: err => {
        this.loading.set(false);
        this.extracting.set(false);
        this.notification.error(extractErrorMessage(err, 'Failed to load chapters.'));
      }
    });
  }

  private scheduleNextPoll(): void {
    clearTimeout(this.pollHandle);
    if (this.extracting() || this.inProgress()) {
      this.pollHandle = setTimeout(() => this.loadChapters(false), POLL_INTERVAL_MS);
    }
  }
}
