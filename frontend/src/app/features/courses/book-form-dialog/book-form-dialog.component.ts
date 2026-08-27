import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { switchMap } from 'rxjs';
import { BookService } from '../../../core/services/book.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';
import { Book } from '../../../core/models/book.model';

export interface BookFormDialogData {
  courseId: number;
  book?: Book;
}

@Component({
  selector: 'app-book-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './book-form-dialog.component.html',
  standalone: true,
  styleUrl: './book-form-dialog.component.css'
})
export class BookFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly bookService = inject(BookService);
  private readonly dialogRef = inject(MatDialogRef<BookFormDialogComponent>);
  protected readonly data = inject<BookFormDialogData>(MAT_DIALOG_DATA);

  protected readonly isEdit = !!this.data.book;
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected selectedFile: File | null = null;
  protected selectedFileName = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    title: [this.data.book?.title ?? '', [Validators.required, Validators.maxLength(255)]]
  });

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selectedFile = file;
    this.selectedFileName.set(file?.name ?? null);
  }

  protected submit(): void {
    if (this.form.invalid || (!this.isEdit && !this.selectedFile)) {
      this.form.markAllAsTouched();
      if (!this.isEdit && !this.selectedFile) {
        this.errorMessage.set('Please choose a file to upload.');
      }
      return;
    }

    const title = this.form.getRawValue().title;
    this.submitting.set(true);
    this.errorMessage.set(null);

    if (this.isEdit) {
      const book = this.data.book!;
      const request$ = this.selectedFile
        ? this.bookService.update(book.id, { title }).pipe(switchMap(() => this.bookService.updateFile(book.id, this.selectedFile!)))
        : this.bookService.update(book.id, { title });

      request$.subscribe({
        next: updated => this.dialogRef.close(updated),
        error: err => {
          this.submitting.set(false);
          this.errorMessage.set(extractErrorMessage(err, 'Failed to update the book.'));
        }
      });
    } else {
      this.bookService.create(title, this.data.courseId, this.selectedFile!).subscribe({
        next: created => this.dialogRef.close(created),
        error: err => {
          this.submitting.set(false);
          this.errorMessage.set(extractErrorMessage(err, 'Failed to add the book.'));
        }
      });
    }
  }

  protected cancel(): void {
    this.dialogRef.close(null);
  }
}
