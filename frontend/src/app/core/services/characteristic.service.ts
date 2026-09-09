import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Characteristic, CharacteristicStatus } from '../models/characteristic.model';
import { ProcessingStatus } from '../models/chapter.model';

export interface CharacteristicGenerationStatus {
  status: ProcessingStatus | null;
  error: string | null;
}

/**
 * Surface characteristics (chapter-scoped) and the shared characteristic-generation
 * trigger/status. Structural characteristics now live under learning objectives — see
 * {@link LearningObjectiveService}.
 */
@Injectable({ providedIn: 'root' })
export class CharacteristicService {
  private readonly http = inject(HttpClient);

  private root(bookId: number, ordinal: number): string {
    return `/api/books/${bookId}/chapters/${ordinal}/characteristics`;
  }

  private surface(bookId: number, ordinal: number): string {
    return `${this.root(bookId, ordinal)}/surface`;
  }

  list(bookId: number, ordinal: number): Observable<Characteristic[]> {
    return this.http.get<Characteristic[]>(this.surface(bookId, ordinal));
  }

  create(bookId: number, ordinal: number, content: string): Observable<Characteristic> {
    return this.http.post<Characteristic>(this.surface(bookId, ordinal), { content });
  }

  update(bookId: number, ordinal: number, id: number, content: string): Observable<Characteristic> {
    return this.http.put<Characteristic>(`${this.surface(bookId, ordinal)}/${id}`, { content });
  }

  updateStatus(
    bookId: number,
    ordinal: number,
    id: number,
    status: CharacteristicStatus
  ): Observable<Characteristic> {
    return this.http.put<Characteristic>(`${this.surface(bookId, ordinal)}/${id}/status`, { status });
  }

  delete(bookId: number, ordinal: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.surface(bookId, ordinal)}/${id}`);
  }

  /** Kicks off async generation of characteristics (202). */
  generate(bookId: number, ordinal: number): Observable<void> {
    return this.http.post<void>(`${this.root(bookId, ordinal)}/generate`, null);
  }

  getGenerationStatus(bookId: number, ordinal: number): Observable<CharacteristicGenerationStatus> {
    return this.http.get<CharacteristicGenerationStatus>(`${this.root(bookId, ordinal)}/generation-status`);
  }
}
