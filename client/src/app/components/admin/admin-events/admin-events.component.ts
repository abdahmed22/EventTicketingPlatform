import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { EventService } from '../../../services/event/event.service';
import { EventStatus, EventSummary } from '../../../models/event.model';
import { ApiError } from '../../../models/api-error.model';

type StatusFilter = EventStatus | 'ALL';

@Component({
  selector: 'app-admin-events',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './admin-events.component.html',
  styleUrl: './admin-events.component.css'
})
export class AdminEventsComponent {
  private readonly eventService = inject(EventService);

  readonly statusOptions: StatusFilter[] = ['ALL', 'DRAFT', 'PUBLISHED', 'CANCELLED'];

  readonly events = signal<EventSummary[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly statusFilter = signal<StatusFilter>('ALL');

  constructor() {
    this.load();
  }

  setFilter(filter: StatusFilter): void {
    this.statusFilter.set(filter);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.success.set(null);

    this.eventService
      .adminList({
        status: this.statusFilter() === 'ALL' ? undefined : (this.statusFilter() as EventStatus),
        size: 100
      })
      .subscribe({
        next: (res) => {
          this.events.set(res.content);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          this.error.set(err instanceof ApiError ? err.message : 'Failed to load events.');
          this.loading.set(false);
        }
      });
  }

  cancelEvent(event: EventSummary): void {
    if (!confirm(`Cancel "${event.title}"? This will cascade-cancel its active bookings.`)) {
      return;
    }
    this.loading.set(true);
    this.eventService.adminCancel(event.id).subscribe({
      next: () => {
        this.success.set(`"${event.title}" was cancelled.`);
        this.load();
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to cancel event.');
        this.loading.set(false);
      }
    });
  }
}
