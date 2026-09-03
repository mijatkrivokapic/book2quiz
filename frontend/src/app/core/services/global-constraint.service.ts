import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { GlobalConstraint } from '../models/global-constraint.model';

@Injectable({ providedIn: 'root' })
export class GlobalConstraintService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/global-constraints';

  list(): Observable<GlobalConstraint[]> {
    return this.http.get<GlobalConstraint[]>(this.baseUrl);
  }

  create(content: string): Observable<GlobalConstraint> {
    return this.http.post<GlobalConstraint>(this.baseUrl, { content });
  }

  update(id: number, content: string): Observable<GlobalConstraint> {
    return this.http.put<GlobalConstraint>(`${this.baseUrl}/${id}`, { content });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
