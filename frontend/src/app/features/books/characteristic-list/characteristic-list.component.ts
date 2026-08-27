import { Component, OnInit, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CharacteristicService } from '../../../core/services/characteristic.service';
import { NotificationService } from '../../../core/services/notification.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';
import { Characteristic, CharacteristicType } from '../../../core/models/characteristic.model';

@Component({
  selector: 'app-characteristic-list',
  imports: [
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  standalone: true,
  templateUrl: './characteristic-list.component.html',
  styleUrl: './characteristic-list.component.css'
})
export class CharacteristicListComponent implements OnInit {
  private readonly service = inject(CharacteristicService);
  private readonly notification = inject(NotificationService);

  readonly bookId = input.required<number>();
  readonly ordinal = input.required<number>();
  readonly type = input.required<CharacteristicType>();
  readonly label = input.required<string>();

  protected readonly items = signal<Characteristic[]>([]);
  protected readonly loading = signal(true);

  protected readonly newContent = signal('');
  protected readonly adding = signal(false);

  protected readonly editingId = signal<number | null>(null);
  protected readonly editContent = signal('');
  protected readonly busyId = signal<number | null>(null);

  ngOnInit(): void {
    this.service.list(this.bookId(), this.ordinal(), this.type()).subscribe({
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

  protected add(): void {
    const content = this.newContent().trim();
    if (!content) {
      return;
    }
    this.adding.set(true);
    this.service.create(this.bookId(), this.ordinal(), this.type(), content).subscribe({
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

  protected startEdit(item: Characteristic): void {
    this.editingId.set(item.id);
    this.editContent.set(item.content);
  }

  protected cancelEdit(): void {
    this.editingId.set(null);
  }

  protected saveEdit(item: Characteristic): void {
    const content = this.editContent().trim();
    if (!content) {
      return;
    }
    this.busyId.set(item.id);
    this.service.update(this.bookId(), this.ordinal(), this.type(), item.id, content).subscribe({
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

  protected remove(item: Characteristic): void {
    this.busyId.set(item.id);
    this.service.delete(this.bookId(), this.ordinal(), this.type(), item.id).subscribe({
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
