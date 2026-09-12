import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { BookService } from '../../../core/services/book.service';
import { ChapterService } from '../../../core/services/chapter.service';
import { GenerationEventsService } from '../../../core/services/generation-events.service';
import { NotificationService } from '../../../core/services/notification.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';
import { Book } from '../../../core/models/book.model';
import { Chapter } from '../../../core/models/chapter.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import {
  AddChapterDialogComponent,
  AddChapterDialogData
} from '../add-chapter-dialog/add-chapter-dialog.component';

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
  private readonly events = inject(GenerationEventsService);
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

  private eventsSub?: Subscription;

  ngOnInit(): void {
    this.loadBook();
    this.loadChapters(true);
    this.eventsSub = this.events.stream(this.bookId).subscribe(msg => {
      if (msg.type === 'connected') {
        this.loadChapters(false); // reconcile on (re)connect
        return;
      }
      const event = msg.event;
      if (event.kind === 'CHAPTER') {
        this.loadChapters(false);
      } else if (event.kind === 'EXTRACTION') {
        this.extracting.set(false);
        if (event.status === 'FAILED') {
          this.notification.error('Chapter extraction failed. Please check the server logs.');
        }
        this.loadChapters(false);
      }
    });
  }

  ngOnDestroy(): void {
    this.eventsSub?.unsubscribe();
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

  protected deleteChapter(chapter: Chapter): void {
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
      this.chapterService.deleteChapter(this.bookId, chapter.ordinal).subscribe({
        next: () => {
          this.chapters.update(list => list.filter(c => c.ordinal !== chapter.ordinal));
          this.notification.success(`Chapter "${chapter.title}" deleted.`);
        },
        error: err => this.notification.error(extractErrorMessage(err, 'Failed to delete the chapter.'))
      });
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
    this.chapterService.extractChapters(this.bookId).subscribe({
      next: () => {
        this.notification.success('Chapter extraction started.');
        // Progress arrives via the SSE stream (CHAPTER / EXTRACTION events).
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
        }
      },
      error: err => {
        this.loading.set(false);
        this.extracting.set(false);
        this.notification.error(extractErrorMessage(err, 'Failed to load chapters.'));
      }
    });
  }
}
