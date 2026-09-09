import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { GenerationRecord, GenerationUsageSummary } from '../models/generation-record.model';

/** Reads a chapter's generation/token-usage history. */
@Injectable({ providedIn: 'root' })
export class GenerationRecordService {
  private readonly http = inject(HttpClient);

  private base(bookId: number, ordinal: number): string {
    return `/api/books/${bookId}/chapters/${ordinal}/generation-records`;
  }

  list(bookId: number, ordinal: number): Observable<GenerationRecord[]> {
    return this.http.get<GenerationRecord[]>(this.base(bookId, ordinal));
  }

  summary(bookId: number, ordinal: number): Observable<GenerationUsageSummary> {
    return this.http.get<GenerationUsageSummary>(`${this.base(bookId, ordinal)}/summary`);
  }
}
