import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Chapter, ChapterSource } from '../models/chapter.model';

@Injectable({ providedIn: 'root' })
export class ChapterService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/books';

  /** Kicks off the async extraction pipeline for a book (returns 202). */
  extractChapters(bookId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${bookId}/extract-chapters`, null);
  }

  /** Re-runs Markdown conversion for a single chapter (returns 202). */
  retryChapter(bookId: number, ordinal: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${bookId}/chapters/${ordinal}/retry`, null);
  }

  getChapters(bookId: number): Observable<Chapter[]> {
    return this.http.get<Chapter[]>(`${this.baseUrl}/${bookId}/chapters`);
  }

  /**
   * Manually creates a chapter. For source 'PDF' the file is converted to Markdown
   * asynchronously (chapter starts PENDING); for 'MARKDOWN' the content is saved
   * directly (chapter is DONE).
   */
  createChapter(
    bookId: number,
    input: { title: string; source: ChapterSource; file?: File | null; content?: string }
  ): Observable<Chapter> {
    const formData = new FormData();
    formData.append('title', input.title);
    formData.append('source', input.source);
    if (input.source === 'PDF' && input.file) {
      formData.append('file', input.file);
    }
    if (input.source === 'MARKDOWN') {
      formData.append('content', input.content ?? '');
    }
    return this.http.post<Chapter>(`${this.baseUrl}/${bookId}/chapters`, formData);
  }

  getChapter(bookId: number, ordinal: number): Observable<Chapter> {
    return this.http.get<Chapter>(`${this.baseUrl}/${bookId}/chapters/${ordinal}`);
  }

  getMarkdown(bookId: number, ordinal: number): Observable<string> {
    return this.http.get(`${this.baseUrl}/${bookId}/chapters/${ordinal}/markdown`, {
      responseType: 'text'
    });
  }

  updateMarkdown(bookId: number, ordinal: number, content: string): Observable<Chapter> {
    return this.http.put<Chapter>(`${this.baseUrl}/${bookId}/chapters/${ordinal}/markdown`, { content });
  }

  /** Direct URL for downloading a chapter PDF (served as an attachment by the backend). */
  chapterPdfUrl(bookId: number, ordinal: number): string {
    return `${this.baseUrl}/${bookId}/chapters/${ordinal}/pdf`;
  }
}
