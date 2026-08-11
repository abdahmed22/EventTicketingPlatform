// ─── Person 2 Component: my-venues.component.ts ─────────────────────────────
// Route: '/organizer/venues'
// Shows the organizer's own venue submissions with their review status.
// Also used by Admins to manage all venues (create auto-APPROVED, edit, view).
// Contains an embedded Signal Form for submitting / updating a venue request.
// ──────────────────────────────────────────────────────────────────────────────
import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { form, FormField, FormRoot, min, required } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import { VenueService } from '../../../services/venue/venue.service';
import { AuthService } from '../../../services/auth/auth.service';
import { VenueCreateRequest, VenueResponse, VenueStatus } from '../../../models/venue.model';
import { ApiError } from '../../../models/api-error.model';

/** Union type for the status filter tabs */
type StatusFilter = VenueStatus | 'ALL';

/** Shape of the reactive model backing the venue Signal Form */
interface VenueFormValue {
  name: string;
  address: string;
  capacity: number;
}

/**
 * Person 2 Component: MyVenuesComponent
 *
 * Serves two audiences:
 *
 *  Organizer view:
 *    - Lists own venue requests (all statuses) with status badges
 *    - Form at the top submits new venue registration requests (PENDING)
 *    - Edit form lets the organizer update their PENDING/REJECTED submission
 *
 *  Admin view (same route, role-aware):
 *    - Lists ALL venues across all organizers
 *    - Form creates a venue directly as APPROVED (no review needed)
 *    - Edit updates any venue
 *
 * The Signal Form handles both create and edit via editingVenueId signal.
 * When editingVenueId is null → create mode. When set → edit mode.
 */

@Component({
  selector: 'app-my-venues',
  standalone: true,
  imports: [DatePipe, FormField, FormRoot],
  templateUrl: './my-venues.component.html',
  styleUrl: './my-venues.component.css'
})
export class MyVenuesComponent {
  private readonly venueService = inject(VenueService);
  readonly authService = inject(AuthService); // public so the template can call isAdmin()

  /** Status filter tab options */
  readonly statusOptions: StatusFilter[] = ['ALL', 'PENDING', 'APPROVED', 'REJECTED'];

  // ─ Signals ─────────────────────────────────────────────────────
  readonly venues = signal<VenueResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly statusFilter = signal<StatusFilter>('ALL');

  /**
   * When null: form is in "create new venue" mode.
   * When set to a venue ID: form is in "edit existing venue" mode.
   */
  readonly editingVenueId = signal<string | null>(null);

  /** Reactive model behind the venue Signal Form; reset to defaults after submit/cancel */
  private readonly venueModel = signal<VenueFormValue>({
    name: '',
    address: '',
    capacity: 500
  });

  /**
   * Angular Signal Form for venue create/edit.
   * Submission logic:
   *   editingVenueId != null → update (PUT /api/organizer/venues/{id} or /api/admin/venues/{id})
   *   editingVenueId == null + Admin → create auto-APPROVED (POST /api/admin/venues)
   *   editingVenueId == null + Organizer → submit for review (POST /api/organizer/venues)
   */
  readonly venueForm = form(
    this.venueModel,
    (path) => {
      required(path.name, { message: 'Venue name is required' });
      required(path.address, { message: 'Address is required' });
      required(path.capacity, { message: 'Capacity is required' });
      min(path.capacity, 1, { message: 'Capacity must be greater than 0' });
    },
    {
      submission: {
        action: async (field) => {
          try {
            this.error.set(null);
            this.successMessage.set(null);
            const val = field().value();
            const req: VenueCreateRequest = {
              name: val.name,
              address: val.address,
              capacity: val.capacity
            };
            const isAdm = this.authService.isAdmin();
            const editId = this.editingVenueId();
            if (editId) {
              // ─ EDIT mode: update via role-appropriate endpoint ────────────────
              const update$ = isAdm
                ? this.venueService.adminUpdate(editId, req)
                : this.venueService.organizerUpdate(editId, req);
              await firstValueFrom(update$);
              this.successMessage.set('Venue updated successfully!');
            } else if (isAdm) {
              // ─ CREATE mode (Admin): auto-APPROVED ────────────────────────────
              await firstValueFrom(this.venueService.adminCreate(req));
              this.successMessage.set('Venue created and approved.');
            } else {
              // ─ CREATE mode (Organizer): submitted for admin review ────────────
              await firstValueFrom(this.venueService.submit(req));
              this.successMessage.set('Venue request submitted successfully! Pending admin approval.');
            }
            this.editingVenueId.set(null);
            this.loadVenues();
            this.venueModel.set({ name: '', address: '', capacity: 500 }); // reset form
            return;
          } catch (err) {
            const message = err instanceof ApiError ? err.message : 'Failed to save venue.';
            return { kind: 'serverError', message };
          }
        }
      }
    }
  );

  constructor() {
    this.loadVenues();
  }

  /** Enters edit mode: populates the form with the selected venue's current values. */
  startEdit(venue: VenueResponse): void {
    this.editingVenueId.set(venue.id);
    this.venueModel.set({
      name: venue.name,
      address: venue.address,
      capacity: venue.capacity
    });
  }

  /** Cancels edit mode and resets the form to empty / create state. */
  cancelEdit(): void {
    this.editingVenueId.set(null);
    this.venueModel.set({ name: '', address: '', capacity: 500 });
  }

  /** Changes the status filter tab and reloads the venue list. */
  setFilter(filter: StatusFilter): void {
    this.statusFilter.set(filter);
    this.loadVenues();
  }

  /**
   * Fetches the venue list using the current status filter.
   * Admin: calls listAdminVenues() → GET /api/admin/venues (all organizers).
   * Organizer: calls listMyVenues() → GET /api/organizer/venues (own venues).
   */
  loadVenues(): void {
    this.loading.set(true);
    this.error.set(null);
    // Undefined status means "no filter" (all statuses)
    const status = this.statusFilter() === 'ALL' ? undefined : (this.statusFilter() as VenueStatus);
    const request$ = this.authService.isAdmin()
      ? this.venueService.listAdminVenues(status)
      : this.venueService.listMyVenues(status);

    request$.subscribe({
      next: (res) => {
        this.venues.set(res);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to load venues.');
        this.loading.set(false);
      }
    });
  }
}