import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { CourseService } from '../../../core/services/course.service';
import { BookService } from '../../../core/services/book.service';
import { NotificationService } from '../../../core/services/notification.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';
import { Course } from '../../../core/models/course.model';
import { Book } from '../../../core/models/book.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { BookFormDialogComponent, BookFormDialogData } from '../book-form-dialog/book-form-dialog.component';

@Component({
  selector: 'app-course-detail',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './course-detail.component.html',
  standalone: true,
  styleUrl: './course-detail.component.css'
})
export class CourseDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly courseService = inject(CourseService);
  private readonly bookService = inject(BookService);
  private readonly notification = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  protected readonly courseId = Number(this.route.snapshot.paramMap.get('id'));
  protected readonly course = signal<Course | null>(null);
  protected readonly books = signal<Book[]>([]);
  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);
  protected readonly booksLoading = signal(false);
  protected readonly deletingBookId = signal<number | null>(null);

  protected readonly editingName = signal(false);
  protected readonly savingName = signal(false);
  protected readonly nameForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255)]]
  });

  protected readonly displayedColumns = ['title', 'actions'];

  ngOnInit(): void {
    this.loadCourse();
    this.loadBooks();
  }

  protected startEditName(): void {
    const course = this.course();
    if (!course) {
      return;
    }
    this.nameForm.setValue({ name: course.name });
    this.editingName.set(true);
  }

  protected cancelEditName(): void {
    this.editingName.set(false);
  }

  protected saveName(): void {
    if (this.nameForm.invalid) {
      this.nameForm.markAllAsTouched();
      return;
    }

    this.savingName.set(true);
    this.courseService.update(this.courseId, this.nameForm.getRawValue()).subscribe({
      next: updated => {
        this.course.set(updated);
        this.savingName.set(false);
        this.editingName.set(false);
        this.notification.success('Course updated.');
      },
      error: err => {
        this.savingName.set(false);
        this.notification.error(extractErrorMessage(err, 'Failed to update the course.'));
      }
    });
  }

  protected openAddBookDialog(): void {
    const data: BookFormDialogData = { courseId: this.courseId };
    this.dialog
      .open(BookFormDialogComponent, { width: '480px', data })
      .afterClosed()
      .subscribe((created: Book | null) => {
        if (!created) {
          return;
        }
        this.books.update(list => [...list, created]);
        this.course.update(c => (c ? { ...c, bookCount: c.bookCount + 1 } : c));
        this.notification.success(`"${created.title}" was added.`);
      });
  }

  protected openEditBookDialog(book: Book): void {
    const data: BookFormDialogData = { courseId: this.courseId, book };
    this.dialog
      .open(BookFormDialogComponent, { width: '480px', data })
      .afterClosed()
      .subscribe((updated: Book | null) => {
        if (!updated) {
          return;
        }
        this.books.update(list => list.map(b => (b.id === updated.id ? updated : b)));
        this.notification.success(`"${updated.title}" was updated.`);
      });
  }

  protected deleteBook(book: Book): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '420px',
      data: { title: 'Delete book', message: `Delete "${book.title}"? This cannot be undone.` }
    });

    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) {
        return;
      }

      this.deletingBookId.set(book.id);
      this.bookService.delete(book.id).subscribe({
        next: () => {
          this.deletingBookId.set(null);
          this.books.update(list => list.filter(b => b.id !== book.id));
          this.course.update(c => (c ? { ...c, bookCount: Math.max(0, c.bookCount - 1) } : c));
          this.notification.success(`"${book.title}" was deleted.`);
        },
        error: err => {
          this.deletingBookId.set(null);
          this.notification.error(extractErrorMessage(err, 'Failed to delete the book.'));
        }
      });
    });
  }

  private loadCourse(): void {
    this.loading.set(true);
    this.courseService.getById(this.courseId).subscribe({
      next: course => {
        this.course.set(course);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.notFound.set(true);
        this.notification.error(extractErrorMessage(err, 'Failed to load the course.'));
      }
    });
  }

  private loadBooks(): void {
    this.booksLoading.set(true);
    this.courseService.getBooksByCourseId(this.courseId).subscribe({
      next: books => {
        this.books.set(books);
        this.booksLoading.set(false);
      },
      error: err => {
        this.booksLoading.set(false);
        this.notification.error(extractErrorMessage(err, 'Failed to load books.'));
      }
    });
  }
}
