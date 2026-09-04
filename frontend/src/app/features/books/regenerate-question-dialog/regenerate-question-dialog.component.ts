import { Component, inject, signal } from '@angular/core';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-regenerate-question-dialog',
  imports: [MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  standalone: true,
  templateUrl: './regenerate-question-dialog.component.html',
  styleUrl: './regenerate-question-dialog.component.css'
})
export class RegenerateQuestionDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<RegenerateQuestionDialogComponent>);

  protected readonly guideline = signal('');

  protected onInput(event: Event): void {
    this.guideline.set((event.target as HTMLTextAreaElement).value);
  }

  protected submit(): void {
    this.dialogRef.close(this.guideline().trim());
  }

  protected cancel(): void {
    this.dialogRef.close(null);
  }
}
