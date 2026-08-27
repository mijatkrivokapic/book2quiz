import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Characteristic, CharacteristicType } from '../models/characteristic.model';

@Injectable({ providedIn: 'root' })
export class CharacteristicService {
  private readonly http = inject(HttpClient);

  private base(bookId: number, ordinal: number, type: CharacteristicType): string {
    return `/api/books/${bookId}/chapters/${ordinal}/characteristics/${type}`;
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

  delete(bookId: number, ordinal: number, type: CharacteristicType, id: number): Observable<void> {
    return this.http.delete<void>(`${this.base(bookId, ordinal, type)}/${id}`);
  }
}
