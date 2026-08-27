import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ChapterService } from '../../../core/services/chapter.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';
import { Chapter, ChapterSource } from '../../../core/models/chapter.model';

export interface AddChapterDialogData {
  bookId: number;
}

@Component({
  selector: 'app-add-chapter-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  standalone: true,
  templateUrl: './add-chapter-dialog.component.html',
  styleUrl: './add-chapter-dialog.component.css'
})
export class AddChapterDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly chapterService = inject(ChapterService);
  private readonly dialogRef = inject(MatDialogRef<AddChapterDialogComponent>);
  protected readonly data = inject<AddChapterDialogData>(MAT_DIALOG_DATA);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly selectedFileName = signal<string | null>(null);
  private selectedFile: File | null = null;

  protected readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    source: ['PDF' as ChapterSource, [Validators.required]],
    content: ['']
  });

  protected readonly source = signal<ChapterSource>('PDF');
  protected readonly isPdf = computed(() => this.source() === 'PDF');

  protected onSourceChange(value: ChapterSource): void {
    this.source.set(value);
    this.form.controls.source.setValue(value);
    this.errorMessage.set(null);
  }

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selectedFile = file;
    this.selectedFileName.set(file?.name ?? null);
  }

  protected submit(): void {
    if (this.form.controls.title.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { title, source, content } = this.form.getRawValue();

    if (source === 'PDF' && !this.selectedFile) {
      this.errorMessage.set('Please choose a PDF file to convert.');
      return;
    }
    if (source === 'MARKDOWN' && !content.trim()) {
      this.errorMessage.set('Please enter some Markdown.');
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    this.chapterService
      .createChapter(this.data.bookId, { title, source, file: this.selectedFile, content })
      .subscribe({
        next: (created: Chapter) => this.dialogRef.close(created),
        error: err => {
          this.submitting.set(false);
          this.errorMessage.set(extractErrorMessage(err, 'Failed to create the chapter.'));
        }
      });
  }

  protected cancel(): void {
    this.dialogRef.close(null);
  }
}
