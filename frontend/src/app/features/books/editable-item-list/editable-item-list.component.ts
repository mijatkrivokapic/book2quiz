import { Component, OnInit, inject, input, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NotificationService } from '../../../core/services/notification.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';

export type ReviewStatus = 'PENDING' | 'APPROVED';

export interface EditableItem {
  id: number;
  content: string;
  status?: ReviewStatus;
  origin?: string;
}

type LoadFn = () => Observable<EditableItem[]>;
type CreateFn = (content: string) => Observable<EditableItem>;
type UpdateFn = (id: number, content: string) => Observable<EditableItem>;
type RemoveFn = (id: number) => Observable<void>;
type StatusFn = (id: number, status: ReviewStatus) => Observable<EditableItem>;

/**
 * Generic inline-editable list of `{ id, content }` items. The parent supplies the
 * CRUD operations as functions, so the same component serves chapter characteristics
 * and course constraints alike.
 */
@Component({
  selector: 'app-editable-item-list',
  imports: [
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  standalone: true,
  templateUrl: './editable-item-list.component.html',
  styleUrl: './editable-item-list.component.css'
})
export class EditableItemListComponent implements OnInit {
  private readonly notification = inject(NotificationService);

  readonly label = input.required<string>();
  readonly load = input.required<LoadFn>();
  readonly create = input.required<CreateFn>();
  readonly update = input.required<UpdateFn>();
  readonly remove = input.required<RemoveFn>();
  /** When true, each item shows its review status and an approve/revert control. */
  readonly approvable = input(false);
  readonly updateStatus = input<StatusFn | undefined>(undefined);

  protected readonly items = signal<EditableItem[]>([]);
  protected readonly loading = signal(true);

  protected readonly newContent = signal('');
  protected readonly adding = signal(false);

  protected readonly editingId = signal<number | null>(null);
  protected readonly editContent = signal('');
  protected readonly busyId = signal<number | null>(null);

  ngOnInit(): void {
    this.reload();
  }

  /** Re-fetches the list (used after external changes such as generation). */
  reload(): void {
    this.loading.set(true);
    this.load()().subscribe({
      next: items => {
        this.items.set(items);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.notification.error(extractErrorMessage(err, `Failed to load ${this.label().toLowerCase()}.`));
      }
    });
  }

  protected setStatus(item: EditableItem, status: ReviewStatus): void {
    const fn = this.updateStatus();
    if (!fn) {
      return;
    }
    this.busyId.set(item.id);
    fn(item.id, status).subscribe({
      next: updated => {
        this.items.update(list => list.map(c => (c.id === updated.id ? updated : c)));
        this.busyId.set(null);
      },
      error: err => {
        this.busyId.set(null);
        this.notification.error(extractErrorMessage(err, 'Failed to update the status.'));
      }
    });
  }

  protected add(): void {
    const content = this.newContent().trim();
    if (!content) {
      return;
    }
    this.adding.set(true);
    this.create()(content).subscribe({
      next: created => {
        this.items.update(list => [...list, created]);
        this.newContent.set('');
        this.adding.set(false);
      },
      error: err => {
        this.adding.set(false);
        this.notification.error(extractErrorMessage(err, 'Failed to add the item.'));
      }
    });
  }

  protected startEdit(item: EditableItem): void {
    this.editingId.set(item.id);
    this.editContent.set(item.content);
  }

  protected cancelEdit(): void {
    this.editingId.set(null);
  }

  protected saveEdit(item: EditableItem): void {
    const content = this.editContent().trim();
    if (!content) {
      return;
    }
    this.busyId.set(item.id);
    this.update()(item.id, content).subscribe({
      next: updated => {
        this.items.update(list => list.map(c => (c.id === updated.id ? updated : c)));
        this.busyId.set(null);
        this.editingId.set(null);
      },
      error: err => {
        this.busyId.set(null);
        this.notification.error(extractErrorMessage(err, 'Failed to update the item.'));
      }
    });
  }

  protected removeItem(item: EditableItem): void {
    this.busyId.set(item.id);
    this.remove()(item.id).subscribe({
      next: () => {
        this.items.update(list => list.filter(c => c.id !== item.id));
        this.busyId.set(null);
      },
      error: err => {
        this.busyId.set(null);
        this.notification.error(extractErrorMessage(err, 'Failed to delete the item.'));
      }
    });
  }

  protected onNewInput(event: Event): void {
    this.newContent.set((event.target as HTMLTextAreaElement).value);
  }

  protected onEditInput(event: Event): void {
    this.editContent.set((event.target as HTMLTextAreaElement).value);
  }
}
