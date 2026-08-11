// ─── Person 2 Component: venue-review.component.ts ───────────────────────────
// Route: '/admin/venues' (Admin only)
// Dedicated admin screen for reviewing organizer-submitted venue requests.
// Shows all venues with status filter tabs; approve/reject actions update the
// venue's status immediately (optimistic UI: re-fetches list after action).
// This is one half of the combined admin review dashboard (§9.4 SRS).
// The other half (organizer applications) is Person 4's OrganizerApplicationsReview.
// ──────────────────────────────────────────────────────────────────────────────
import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { VenueService } from '../../../services/venue/venue.service';
import { VenueResponse, VenueStatus } from '../../../models/venue.model';
import { ApiError } from '../../../models/api-error.model';

/** Union type for the status filter tabs */
type StatusFilter = VenueStatus | 'ALL';

/**
 * Person 2 Component: VenueReviewComponent
 *
 * Admin-only screen. Lists all venue registration requests, defaulting to PENDING
 * so the admin sees what needs action first.
 *
 * Actions:
 *   - approve(id): POST /api/admin/venues/{id}/approve → status becomes APPROVED
 *                  The venue now appears in the event-creation dropdown for all organizers.
 *   - reject(id):  POST /api/admin/venues/{id}/reject  → status becomes REJECTED
 *                  The row is kept as a permanent record; the venue is never usable.
 *
 * actioningId tracks which row's button is in-flight so the UI can show a spinner
 * on just that row without disabling the whole list.
 *
 * Note: this component is also data-source for Person 4's AdminDashboardComponent
 * which calls VenueService.listAdminVenues('PENDING') to show pending venues in the
 * combined pending-queues panel (§9.4 SRS).
 */

@Component({
  selector: 'app-venue-review',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './venue-review.component.html',
  styleUrl: './venue-review.component.css'
})
export class VenueReviewComponent {
  private readonly venueService = inject(VenueService);

  /**
   * Status filter tabs. Defaults to PENDING on load so the admin
   * immediately sees the queue that needs action.
   */
  readonly statusOptions: StatusFilter[] = ['PENDING', 'APPROVED', 'REJECTED', 'ALL'];

  readonly venues = signal<VenueResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly statusFilter = signal<StatusFilter>('PENDING'); // default to PENDING queue

  /**
   * Tracks the ID of the venue currently being approved/rejected.
   * Used to show a per-row loading indicator while the HTTP call is in-flight.
   * Null when no action is pending.
   */
  readonly actioningId = signal<string | null>(null);

  constructor() {
    this.load(); // fetch PENDING venues on init
  }

  /** Changes the status filter tab and reloads the venue list. */
  setFilter(filter: StatusFilter): void {
    this.statusFilter.set(filter);
    this.load();
  }

  /**
   * Fetches venues from GET /api/admin/venues[?status=].
   * When statusFilter is 'ALL', no status param is sent (returns every venue).
   */
  load(): void {
    this.loading.set(true);
    this.error.set(null);
    // Undefined means no status filter — backend returns all
    const status = this.statusFilter() === 'ALL' ? undefined : (this.statusFilter() as VenueStatus);

    this.venueService.listAdminVenues(status).subscribe({
      next: (res) => {
        this.venues.set(res);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to load venues for review.');
        this.loading.set(false);
      }
    });
  }

  /**
   * Approves the venue: POST /api/admin/venues/{id}/approve
   * Sets actioningId while in-flight to show a per-row spinner.
   * Reloads the list on success (the venue disappears from PENDING view).
   */
  approve(id: string): void {
    this.actioningId.set(id);
    this.error.set(null);

    this.venueService.approve(id).subscribe({
      next: () => {
        this.actioningId.set(null);
        this.load(); // refresh so the approved venue moves out of the PENDING tab
      },
      error: (err: unknown) => {
        this.actioningId.set(null);
        this.error.set(err instanceof ApiError ? err.message : 'Failed to approve venue.');
      }
    });
  }

  /**
   * Rejects the venue: POST /api/admin/venues/{id}/reject
   * The venue record is kept as a permanent audit trail; it can never be
   * used for event creation. An optional rejection reason can be passed
   * (currently not prompted in the UI — extend if needed).
   */
  reject(id: string): void {
    this.actioningId.set(id);
    this.error.set(null);

    this.venueService.reject(id).subscribe({
      next: () => {
        this.actioningId.set(null);
        this.load(); // refresh so the rejected venue moves out of the PENDING tab
      },
      error: (err: unknown) => {
        this.actioningId.set(null);
        this.error.set(err instanceof ApiError ? err.message : 'Failed to reject venue.');
      }
    });
  }
}
