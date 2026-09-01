import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Characteristic, CharacteristicStatus, CharacteristicType } from '../models/characteristic.model';
import { ProcessingStatus } from '../models/chapter.model';

export interface CharacteristicGenerationStatus {
  status: ProcessingStatus | null;
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class CharacteristicService {
  private readonly http = inject(HttpClient);

  private root(bookId: number, ordinal: number): string {
    return `/api/books/${bookId}/chapters/${ordinal}/characteristics`;
  }

  private base(bookId: number, ordinal: number, type: CharacteristicType): string {
    return `${this.root(bookId, ordinal)}/${type}`;
  }

  list(bookId: number, ordinal: number, type: CharacteristicType): Observable<Characteristic[]> {
    return this.http.get<Characteristic[]>(this.base(bookId, ordinal, type));
  }

  create(bookId: number, ordinal: number, type: CharacteristicType, content: string): Observable<Characteristic> {
    return this.http.post<Characteristic>(this.base(bookId, ordinal, type), { content });
  }

  update(
    bookId: number,
    ordinal: number,
    type: CharacteristicType,
    id: number,
    content: string
  ): Observable<Characteristic> {
    return this.http.put<Characteristic>(`${this.base(bookId, ordinal, type)}/${id}`, { content });
  }

  updateStatus(
    bookId: number,
    ordinal: number,
    type: CharacteristicType,
    id: number,
    status: CharacteristicStatus
  ): Observable<Characteristic> {
    return this.http.put<Characteristic>(`${this.base(bookId, ordinal, type)}/${id}/status`, { status });
  }

  delete(bookId: number, ordinal: number, type: CharacteristicType, id: number): Observable<void> {
    return this.http.delete<void>(`${this.base(bookId, ordinal, type)}/${id}`);
  }

  /** Kicks off async generation of both structural and surface characteristics (202). */
  generate(bookId: number, ordinal: number): Observable<void> {
    return this.http.post<void>(`${this.root(bookId, ordinal)}/generate`, null);
  }

  getGenerationStatus(bookId: number, ordinal: number): Observable<CharacteristicGenerationStatus> {
    return this.http.get<CharacteristicGenerationStatus>(`${this.root(bookId, ordinal)}/generation-status`);
  }
}
