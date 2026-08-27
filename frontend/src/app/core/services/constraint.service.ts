import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Constraint } from '../models/constraint.model';

@Injectable({ providedIn: 'root' })
export class ConstraintService {
  private readonly http = inject(HttpClient);

  private base(bookId: number, ordinal: number): string {
    return `/api/books/${bookId}/chapters/${ordinal}/constraints`;
  }

  list(bookId: number, ordinal: number): Observable<Constraint[]> {
    return this.http.get<Constraint[]>(this.base(bookId, ordinal));
  }

  create(bookId: number, ordinal: number, content: string): Observable<Constraint> {
    return this.http.post<Constraint>(this.base(bookId, ordinal), { content });
  }

  update(bookId: number, ordinal: number, id: number, content: string): Observable<Constraint> {
    return this.http.put<Constraint>(`${this.base(bookId, ordinal)}/${id}`, { content });
  }

  delete(bookId: number, ordinal: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.base(bookId, ordinal)}/${id}`);
  }
}
