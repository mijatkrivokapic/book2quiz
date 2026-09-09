import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Characteristic, CharacteristicStatus, LearningObjective } from '../models/characteristic.model';

/**
 * Learning objectives of a chapter and, nested under each, its structural characteristics.
 */
@Injectable({ providedIn: 'root' })
export class LearningObjectiveService {
  private readonly http = inject(HttpClient);

  private base(bookId: number, ordinal: number): string {
    return `/api/books/${bookId}/chapters/${ordinal}/learning-objectives`;
  }

  private structuralBase(bookId: number, ordinal: number, loId: number): string {
    return `${this.base(bookId, ordinal)}/${loId}/structural-characteristics`;
  }

  // ---- Learning objectives -------------------------------------------------------------

  list(bookId: number, ordinal: number): Observable<LearningObjective[]> {
    return this.http.get<LearningObjective[]>(this.base(bookId, ordinal));
  }

  create(bookId: number, ordinal: number, description: string): Observable<LearningObjective> {
    return this.http.post<LearningObjective>(this.base(bookId, ordinal), { description });
  }

  update(bookId: number, ordinal: number, loId: number, description: string): Observable<LearningObjective> {
    return this.http.put<LearningObjective>(`${this.base(bookId, ordinal)}/${loId}`, { description });
  }

  updateStatus(
    bookId: number,
    ordinal: number,
    loId: number,
    status: CharacteristicStatus
  ): Observable<LearningObjective> {
    return this.http.put<LearningObjective>(`${this.base(bookId, ordinal)}/${loId}/status`, { status });
  }

  delete(bookId: number, ordinal: number, loId: number): Observable<void> {
    return this.http.delete<void>(`${this.base(bookId, ordinal)}/${loId}`);
  }

  // ---- Structural characteristics (nested under a learning objective) ------------------

  listStructural(bookId: number, ordinal: number, loId: number): Observable<Characteristic[]> {
    return this.http.get<Characteristic[]>(this.structuralBase(bookId, ordinal, loId));
  }

  createStructural(bookId: number, ordinal: number, loId: number, content: string): Observable<Characteristic> {
    return this.http.post<Characteristic>(this.structuralBase(bookId, ordinal, loId), { content });
  }

  updateStructural(
    bookId: number,
    ordinal: number,
    loId: number,
    id: number,
    content: string
  ): Observable<Characteristic> {
    return this.http.put<Characteristic>(`${this.structuralBase(bookId, ordinal, loId)}/${id}`, { content });
  }

  updateStructuralStatus(
    bookId: number,
    ordinal: number,
    loId: number,
    id: number,
    status: CharacteristicStatus
  ): Observable<Characteristic> {
    return this.http.put<Characteristic>(`${this.structuralBase(bookId, ordinal, loId)}/${id}/status`, { status });
  }

  deleteStructural(bookId: number, ordinal: number, loId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.structuralBase(bookId, ordinal, loId)}/${id}`);
  }
}
