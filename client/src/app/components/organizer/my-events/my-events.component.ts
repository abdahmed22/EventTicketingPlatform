// ─── Person 2 Component: my-events.component.ts ─────────────────────────────
// Route: '/organizer/events'
// Shows the list of events the logged-in organizer owns (all statuses).
// Admin users also land here and see ALL events across all organizers.
// ──────────────────────────────────────────────────────────────────────────────
import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { EventService } from '../../../services/event/event.service';
import { AuthService } from '../../../services/auth/auth.service';
import { EventStatus, EventSummary } from '../../../models/event.model';
import { ApiError } from '../../../models/api-error.model';

/** Union type for the status filter tabs: any EventStatus or 'ALL' */
type StatusFilter = EventStatus | 'ALL';

/**
 * Person 2 Component: MyEventsComponent
 *
 * Dashboard listing for events the organizer owns.
 * Role-aware behaviour:
 *   - Organizer: calls listOrganizerEvents() → GET /api/organizer/events
 *   - Admin:     calls adminList()           → GET /api/admin/events (all events)
 *
 * Supports status filter tabs (ALL / DRAFT / PUBLISHED / CANCELLED).
 * Provides quick-action buttons (Publish, Cancel) without navigating away.
 */

@Component({
  selector: 'app-my-events',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './my-events.component.html',
  styleUrl: './my-events.component.css'
})
export class MyEventsComponent {
  private readonly eventService = inject(EventService);
  readonly authService = inject(AuthService); // public so the template can call isAdmin()

  /** Status filter tab options shown above the event table */
  readonly statusOptions: StatusFilter[] = ['ALL', 'DRAFT', 'PUBLISHED', 'CANCELLED'];

  // ─ Signals ──────────────────────────────────────────────────────
  readonly events = signal<EventSummary[]>([]);           // current page of events
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);  // shown after publish / cancel
  readonly statusFilter = signal<StatusFilter>('ALL');    // currently active filter tab

  constructor() {
    this.loadEvents();
  }

  /** Changes the active status filter tab and reloads the list. */
  setFilter(filter: StatusFilter): void {
    this.statusFilter.set(filter);
    this.loadEvents();
  }

  /**
   * Fetches the events list using the current status filter.
   * Organizer: hits /api/organizer/events (own events only, all statuses).
   * Admin:     hits /api/admin/events (all events, all organizers).
   * organizerId filter is not sent for Admin — backend returns all events.
   */
  loadEvents(): void {
    this.loading.set(true);
    this.error.set(null);
    this.successMessage.set(null);

    const isAdm = this.authService.isAdmin();
    const userId = this.authService.user()?.id;

    const filters = {
      status: this.statusFilter() === 'ALL' ? undefined : (this.statusFilter() as EventStatus),
      organizerId: isAdm ? undefined : userId, // restrict to own events for organizer
      size: 50
    };

    // Choose the correct endpoint based on role
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

  /**
   * Publishes a DRAFT event (POST /api/organizer/events/{id}/publish).
   * Reloads the list on success so the status badge updates immediately.
   */
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

  /**
   * Cancels an event with a confirmation dialog.
   * Admin uses adminCancel() (any event), organizer uses organizerCancel() (own only).
   * Backend cascade-cancels all active bookings for the event.
   */
  cancelEvent(id: string): void {
    if (!confirm('Are you sure you want to cancel this event?')) return;
    this.loading.set(true);
    // Route to the correct cancel endpoint based on role
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
