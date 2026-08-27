import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Book, UpdateBookRequest } from '../models/book.model';

@Injectable({ providedIn: 'root' })
export class BookService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/books';

  getAll(): Observable<Book[]> {
    return this.http.get<Book[]>(this.baseUrl);
  }

  getById(id: number): Observable<Book> {
    return this.http.get<Book>(`${this.baseUrl}/${id}`);
  }

  create(title: string, courseId: number, file: File): Observable<Book> {
    const formData = new FormData();
    formData.append('title', title);
    formData.append('courseId', String(courseId));
    formData.append('file', file);
    return this.http.post<Book>(this.baseUrl, formData);
  }

  update(id: number, request: UpdateBookRequest): Observable<Book> {
    return this.http.put<Book>(`${this.baseUrl}/${id}`, request);
  }

  updateFile(id: number, file: File): Observable<Book> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.put<Book>(`${this.baseUrl}/${id}/file`, formData);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
