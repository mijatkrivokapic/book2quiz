import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { CourseService } from '../../../core/services/course.service';
import { NotificationService } from '../../../core/services/notification.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';
import { Course } from '../../../core/models/course.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-course-list',
  imports: [
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './course-list.component.html',
  standalone: true,
  styleUrl: './course-list.component.css'
})
export class CourseListComponent implements OnInit {
  private readonly courseService = inject(CourseService);
  private readonly notification = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);

  protected readonly courses = signal<Course[]>([]);
  protected readonly loading = signal(true);
  protected readonly deletingId = signal<number | null>(null);
  protected readonly displayedColumns = ['name', 'bookCount', 'lessonCount', 'actions'];

  ngOnInit(): void {
    this.load();
  }

  protected openCourse(course: Course): void {
    this.router.navigate(['/courses', course.id]);
  }

  protected deleteCourse(course: Course, event: Event): void {
    event.stopPropagation();

    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '420px',
      data: {
        title: 'Delete course',
        message: `Delete "${course.name}"? Its ${course.bookCount} book(s) and ${course.lessonCount} lesson(s) will be deleted too. This cannot be undone.`
      }
    });

    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) {
        return;
      }

      this.deletingId.set(course.id);
      this.courseService.delete(course.id).subscribe({
        next: () => {
          this.deletingId.set(null);
          this.courses.update(list => list.filter(c => c.id !== course.id));
          this.notification.success(`"${course.name}" was deleted.`);
        },
        error: err => {
          this.deletingId.set(null);
          this.notification.error(extractErrorMessage(err, 'Failed to delete the course.'));
        }
      });
    });
  }

  private load(): void {
    this.loading.set(true);
    this.courseService.getAll().subscribe({
      next: courses => {
        this.courses.set(courses);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.notification.error(extractErrorMessage(err, 'Failed to load courses.'));
      }
    });
  }
}
