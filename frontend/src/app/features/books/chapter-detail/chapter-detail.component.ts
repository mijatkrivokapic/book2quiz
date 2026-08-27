import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BookService } from '../../../core/services/book.service';
import { ChapterService } from '../../../core/services/chapter.service';
import { NotificationService } from '../../../core/services/notification.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';
import { renderMarkdown } from '../../../core/utils/markdown.util';
import { Book } from '../../../core/models/book.model';
import { Chapter } from '../../../core/models/chapter.model';

@Component({
  selector: 'app-chapter-detail',
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  standalone: true,
  templateUrl: './chapter-detail.component.html',
  styleUrl: './chapter-detail.component.css'
})
export class ChapterDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly bookService = inject(BookService);
  private readonly chapterService = inject(ChapterService);
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
