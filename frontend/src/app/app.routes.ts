import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'courses', pathMatch: 'full' },
  {
    path: 'courses',
    loadComponent: () =>
      import('./features/courses/course-list/course-list.component').then(m => m.CourseListComponent)
  },
  {
    path: 'courses/new',
    loadComponent: () =>
      import('./features/courses/course-form/course-form.component').then(m => m.CourseFormComponent)
  },
  {
    path: 'courses/:id',
    loadComponent: () =>
      import('./features/courses/course-detail/course-detail.component').then(m => m.CourseDetailComponent)
  },
  {
    path: 'books/:bookId/chapters',
    loadComponent: () =>
      import('./features/books/book-chapters/book-chapters.component').then(m => m.BookChaptersComponent)
  },
  {
    path: 'books/:bookId/chapters/:ordinal',
    loadComponent: () =>
      import('./features/books/chapter-detail/chapter-detail.component').then(m => m.ChapterDetailComponent)
  },
  { path: '**', redirectTo: 'courses' }
];
