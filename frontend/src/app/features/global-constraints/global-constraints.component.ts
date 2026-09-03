import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { GlobalConstraintService } from '../../core/services/global-constraint.service';
import { EditableItemListComponent } from '../books/editable-item-list/editable-item-list.component';

/**
 * App-wide (global/base) quiz constraints. These are prepended to every quiz generation
 * before any per-chapter constraints.
 */
@Component({
  selector: 'app-global-constraints',
  imports: [RouterLink, MatIconModule, EditableItemListComponent],
  standalone: true,
  templateUrl: './global-constraints.component.html',
  styleUrl: './global-constraints.component.css'
})
export class GlobalConstraintsComponent {
  private readonly service = inject(GlobalConstraintService);

  protected readonly load = () => this.service.list();
  protected readonly create = (content: string) => this.service.create(content);
  protected readonly update = (id: number, content: string) => this.service.update(id, content);
  protected readonly remove = (id: number) => this.service.delete(id);
}
