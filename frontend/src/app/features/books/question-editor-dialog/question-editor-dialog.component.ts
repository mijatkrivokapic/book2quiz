import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MrqOption, QuestionPayload, QuestionType } from '../../../core/models/question.model';

export interface QuestionEditorDialogData {
  question?: QuestionPayload;
}

@Component({
  selector: 'app-question-editor-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatCheckboxModule,
    MatIconModule
  ],
  standalone: true,
  templateUrl: './question-editor-dialog.component.html',
  styleUrl: './question-editor-dialog.component.css'
})
export class QuestionEditorDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<QuestionEditorDialogComponent>);
  private readonly data = inject<QuestionEditorDialogData>(MAT_DIALOG_DATA);

  protected readonly isEdit = !!this.data.question;
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly type = signal<QuestionType>(this.data.question?.questionType ?? 'MULTIPLE_CHOICE');

  protected readonly typeOptions: { value: QuestionType; label: string }[] = [
    { value: 'MULTIPLE_CHOICE', label: 'Multiple choice' },
    { value: 'MULTIPLE_RESPONSE', label: 'Multiple response' },
    { value: 'SHORT_ANSWER', label: 'Short answer' }
  ];

  protected form: FormGroup = this.buildForm(this.type(), this.data.question);

  protected onTypeChange(value: QuestionType): void {
    this.type.set(value);
    this.form = this.buildForm(value, this.data.question?.questionType === value ? this.data.question : undefined);
    this.errorMessage.set(null);
  }

  private buildForm(type: QuestionType, existing?: QuestionPayload): FormGroup {
    const group = new FormGroup<{ [key: string]: AbstractControl }>({});
    group.addControl('text', this.textControl(existing?.text, true));
    group.addControl('hints', this.controlArray(existing?.hints));

    if (type === 'MULTIPLE_CHOICE') {
      group.addControl('correctOption', this.textControl(existing?.correctOption, true));
      group.addControl('feedback', this.textControl(existing?.feedback));
      group.addControl('distractors', this.controlArray(existing?.distractors, 2));
    } else if (type === 'MULTIPLE_RESPONSE') {
      const options = existing?.options?.length
        ? existing.options.map(o => this.optionGroup(o))
        : [this.optionGroup(), this.optionGroup()];
      group.addControl('options', new FormArray(options));
    } else {
      group.addControl('feedback', this.textControl(existing?.feedback));
      group.addControl('acceptableAnswers', this.controlArray(existing?.acceptableAnswers, 1));
    }
    return group;
  }

  private textControl(value?: string, required = false): FormControl<string> {
    return new FormControl(value ?? '', {
      nonNullable: true,
      validators: required ? [Validators.required] : []
    });
  }

  private controlArray(values: string[] | undefined, minRows = 0): FormArray {
    const rows = (values && values.length ? values : Array(minRows).fill(''))
      .map(v => this.textControl(v));
    return new FormArray<FormControl<string>>(rows);
  }

  private optionGroup(option?: MrqOption): FormGroup {
    return new FormGroup({
      text: this.textControl(option?.text, true),
      isCorrect: new FormControl(option?.isCorrect ?? false, { nonNullable: true }),
      feedback: this.textControl(option?.feedback)
    });
  }

  // ---- template helpers ----

  protected array(name: string): FormArray {
    return this.form.get(name) as FormArray;
  }

  protected control(name: string): FormControl {
    return this.form.get(name) as FormControl;
  }

  protected addRow(name: string): void {
    this.array(name).push(this.textControl(''));
  }

  protected addOption(): void {
    this.array('options').push(this.optionGroup());
  }

  protected removeAt(name: string, index: number): void {
    this.array(name).removeAt(index);
  }

  protected submit(): void {
    if (this.control('text').invalid) {
      this.form.markAllAsTouched();
      this.errorMessage.set('Question text is required.');
      return;
    }

    const type = this.type();
    const text = (this.control('text').value as string).trim();
    const hints = this.cleanList('hints');

    let payload: QuestionPayload;
    if (type === 'MULTIPLE_CHOICE') {
      const distractors = this.cleanList('distractors');
      const correctOption = (this.control('correctOption').value as string).trim();
      if (!correctOption) {
        this.errorMessage.set('A correct option is required.');
        return;
      }
      if (distractors.length === 0) {
        this.errorMessage.set('Add at least one distractor.');
        return;
      }
      if (distractors.some(d => d.toLowerCase() === correctOption.toLowerCase())) {
        this.errorMessage.set('The correct option must not appear among the distractors.');
        return;
      }
      payload = { questionType: type, text, hints, distractors, correctOption, feedback: this.feedback() };
    } else if (type === 'MULTIPLE_RESPONSE') {
      const options: MrqOption[] = this.array('options').controls
        .map(c => c.value as { text: string; isCorrect: boolean; feedback: string })
        .filter(o => o.text.trim().length > 0)
        .map(o => ({ text: o.text.trim(), isCorrect: o.isCorrect, feedback: o.feedback ?? '', hints: [] }));
      if (options.length === 0) {
        this.errorMessage.set('Add at least one option.');
        return;
      }
      if (!options.some(o => o.isCorrect)) {
        this.errorMessage.set('At least one option must be marked correct.');
        return;
      }
      payload = { questionType: type, text, hints, options };
    } else {
      const acceptableAnswers = this.cleanList('acceptableAnswers');
      if (acceptableAnswers.length === 0) {
        this.errorMessage.set('Add at least one acceptable answer.');
        return;
      }
      payload = { questionType: type, text, hints, acceptableAnswers, feedback: this.feedback() };
    }

    this.dialogRef.close(payload);
  }

  protected cancel(): void {
    this.dialogRef.close(null);
  }

  private feedback(): string {
    const control = this.form.get('feedback');
    return control ? ((control.value as string) ?? '').trim() : '';
  }

  private cleanList(name: string): string[] {
    return this.array(name).controls
      .map(c => (c.value as string).trim())
      .filter(v => v.length > 0);
  }
}
