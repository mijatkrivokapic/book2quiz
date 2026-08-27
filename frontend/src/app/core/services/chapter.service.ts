import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Chapter } from '../models/chapter.model';

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
