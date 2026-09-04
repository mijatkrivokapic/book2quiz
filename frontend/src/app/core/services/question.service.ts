import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Question, QuestionPayload, QuestionStatus, QuestionVersion } from '../models/question.model';
import { ProcessingStatus } from '../models/chapter.model';

export interface QuestionGenerationStatus {
  status: ProcessingStatus | null;
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class QuestionService {
  private readonly http = inject(HttpClient);

  private base(bookId: number, ordinal: number): string {
    return `/api/books/${bookId}/chapters/${ordinal}/questions`;
  }

  list(bookId: number, ordinal: number): Observable<Question[]> {
    return this.http.get<Question[]>(this.base(bookId, ordinal));
  }

  /** Kicks off async generation from the chapter's material (returns 202). */
  generate(bookId: number, ordinal: number): Observable<void> {
    return this.http.post<void>(`${this.base(bookId, ordinal)}/generate`, null);
  }

  getGenerationStatus(bookId: number, ordinal: number): Observable<QuestionGenerationStatus> {
    return this.http.get<QuestionGenerationStatus>(`${this.base(bookId, ordinal)}/generation-status`);
  }

  create(bookId: number, ordinal: number, question: QuestionPayload): Observable<Question> {
    return this.http.post<Question>(this.base(bookId, ordinal), { question });
  }

  update(bookId: number, ordinal: number, id: number, question: QuestionPayload): Observable<Question> {
    return this.http.put<Question>(`${this.base(bookId, ordinal)}/${id}`, { question });
  }

  updateStatus(bookId: number, ordinal: number, id: number, status: QuestionStatus): Observable<Question> {
    return this.http.put<Question>(`${this.base(bookId, ordinal)}/${id}/status`, { status });
  }

  delete(bookId: number, ordinal: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.base(bookId, ordinal)}/${id}`);
  }

  /** Kicks off async regeneration of one question with an optional guideline (202). */
  regenerate(bookId: number, ordinal: number, id: number, guideline: string): Observable<void> {
    return this.http.post<void>(`${this.base(bookId, ordinal)}/${id}/regenerate`, { guideline });
  }

  listVersions(bookId: number, ordinal: number, id: number): Observable<QuestionVersion[]> {
    return this.http.get<QuestionVersion[]>(`${this.base(bookId, ordinal)}/${id}/versions`);
  }

  activateVersion(bookId: number, ordinal: number, id: number, versionId: number): Observable<Question> {
    return this.http.put<Question>(`${this.base(bookId, ordinal)}/${id}/versions/${versionId}/activate`, null);
  }

  updateVersion(
    bookId: number,
    ordinal: number,
    id: number,
    versionId: number,
    question: QuestionPayload
  ): Observable<QuestionVersion> {
    return this.http.put<QuestionVersion>(
      `${this.base(bookId, ordinal)}/${id}/versions/${versionId}`,
      { question }
    );
  }

  deleteVersion(bookId: number, ordinal: number, id: number, versionId: number): Observable<void> {
    return this.http.delete<void>(`${this.base(bookId, ordinal)}/${id}/versions/${versionId}`);
  }
}
