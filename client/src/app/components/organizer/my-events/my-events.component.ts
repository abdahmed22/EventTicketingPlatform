import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { EventService } from '../../../services/event/event.service';
import { AuthService } from '../../../services/auth/auth.service';
import { EventStatus, EventSummary } from '../../../models/event.model';
import { ApiError } from '../../../models/api-error.model';

type StatusFilter = EventStatus | 'ALL';

@Component({
  selector: 'app-my-events',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './my-events.component.html',
  styleUrl: './my-events.component.css'
})
export class MyEventsComponent {
  private readonly eventService = inject(EventService);
  readonly authService = inject(AuthService);

  readonly statusOptions: StatusFilter[] = ['ALL', 'DRAFT', 'PUBLISHED', 'CANCELLED'];

  readonly events = signal<EventSummary[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly statusFilter = signal<StatusFilter>('ALL');

  constructor() {
    this.loadEvents();
  }

  setFilter(filter: StatusFilter): void {
    this.statusFilter.set(filter);
    this.loadEvents();
  }

  loadEvents(): void {
    this.loading.set(true);
    this.error.set(null);
    this.successMessage.set(null);

    const isAdm = this.authService.isAdmin();
    const userId = this.authService.user()?.id;

    const filters = {
      status: this.statusFilter() === 'ALL' ? undefined : (this.statusFilter() as EventStatus),
      organizerId: isAdm ? undefined : userId,
      size: 50
    };

    const request$ = isAdm
      ? this.eventService.adminList(filters)
      : this.eventService.listOrganizerEvents(filters);

    request$.subscribe({
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

  publishEvent(id: string): void {
    this.loading.set(true);
    this.eventService.publish(id).subscribe({
      next: () => {
        this.successMessage.set('Event published successfully!');
        this.loadEvents();
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to publish event.');
        this.loading.set(false);
      }
    });
  }

  cancelEvent(id: string): void {
    if (!confirm('Are you sure you want to cancel this event?')) return;
    this.loading.set(true);
    const request$ = this.authService.isAdmin()
      ? this.eventService.adminCancel(id)
      : this.eventService.organizerCancel(id);

    request$.subscribe({
      next: () => {
        this.successMessage.set('Event cancelled.');
        this.loadEvents();
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to cancel event.');
        this.loading.set(false);
      }
    });
  }
}
