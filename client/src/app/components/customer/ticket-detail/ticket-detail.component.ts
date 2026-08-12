
import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { form, FormField, FormRoot, min, required } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';

import { EventService } from '../../../services/event/event.service';
import { SeatCategoryService } from '../../../services/seat-category/seat-category.service';
import { BookingService } from '../../../services/booking/booking.service';
import { AuthService } from '../../../services/auth/auth.service';

import { EventCategory, EventResponse } from '../../../models/event.model';
import { SeatCategoryCreateRequest, SeatCategorySummary } from '../../../models/seat-category.model';
import { BookingResponse } from '../../../models/booking.model';
import { ApiError } from '../../../models/api-error.model';
import { getEventCategoryImage } from '../../../utils/event-image.util';

/** Shape of the reactive model backing the "Add Seat Category" Signal Form */
interface SeatCategoryFormValue {
  name: string;
  price: number;
  totalSeats: number;
  seatingCapacity: number;
}

/**
 *
 * Renders a single event's full detail. The component is role-aware:
 *
 *  Customer view (PUBLISHED event):
 *    - Hero image, description, venue card, seat category ticket options
 *    - Booking widget: seat-category dropdown + quantity stepper + Reserve button
 *      (the actual POST /api/customer/bookings is delegated to BookingService,
 *       Person 3's concern — this component just calls bookingService.reserve())
 *
 *  Organizer / Admin view (any status):
 *    - Status pill, management actions bar (Publish, Cancel, Add Seat Category)
 *    - Collapsible attendees/bookings panel
 *    - "Add Seat Category" modal (Signal Form — posts to SeatCategoryService)
 *
 * The correct EventService method (public / organizer / admin) is chosen
 * based on authService role inside loadEvent().
 */

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [DatePipe, CurrencyPipe, RouterLink, FormField, FormRoot],
  templateUrl: './event-detail.component.html',
  styleUrl: './event-detail.component.css',
})
export class EventDetailComponent {
  // Expose Math/Number to the template (Angular templates can't call globals directly)
  readonly Number = Number;
  readonly Math = Math;

  /**
   * Calculates the maximum tickets a customer can add to their order
   * for the given seat category. Respects seatingCapacity (multi-person seats)
   * and availableSeats (live count from the server).
   */
  getMaxQuantity(cat: SeatCategorySummary | null): number {
    if (!cat) return 1;
    if (cat.seatingCapacity && cat.seatingCapacity > 0) {
      return Math.min(cat.availableSeats, cat.seatingCapacity);
    }
    return cat.availableSeats;
  }

  // avoids a long constructor
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventService = inject(EventService);
  private readonly seatCategoryService = inject(SeatCategoryService);
  private readonly bookingService = inject(BookingService);
  readonly authService = inject(AuthService); // public so the template can check role

  // ─ Core data signals ───────────────────────────────────────────────
  readonly event = signal<EventResponse | null>(null); // the loaded event
  readonly loading = signal(true); // is true we show loading message / fetching
  readonly error = signal<string | null>(null); // error banner message
  readonly actionSuccess = signal<string | null>(null); // success banner message

  // ─ Organizer/Admin: bookings panel ───────────────────────────────────
  readonly bookings = signal<BookingResponse[]>([]); // attendees list
  readonly bookingsLoading = signal(false);
  readonly bookingsError = signal<string | null>(null);
  readonly showBookings = signal(false); // toggles the panel visibility

  // ─ Customer: booking widget state ─────────────────────────────────
  readonly selectedSeatCategoryId = signal<string>(''); // which category the customer picked
  readonly quantity = signal<number>(1); // how many tickets they want
  readonly reserving = signal<boolean>(false); // true while reserve() HTTP call is in-flight

  /**
   * Derived: resolves the selected seat category object from the event's
   * seatCategories array. Used by the template for price calculation and
   * availability badge display.
   */

  //It looks up the full seat category object that matches whatever
  // id the customer currently has selected in a dropdown/radio list.

  readonly selectedCategory = computed(() => {
    const categoryId = this.selectedSeatCategoryId();
    const evt = this.event();
    if (!evt || !evt.seatCategories) return null;
    return evt.seatCategories.find((cat) => cat.id === categoryId) || null;
  });

  // ─ Organizer: "Add Seat Category" modal state ────────────────────────
  readonly showSeatModal = signal(false); // controls modal visibility
  readonly submittingCategory = signal(false); // true while the form is being submitted

  /**
   * Reactive model backing the seat category Signal Form.
   * Reset to defaults each time the modal is closed.
   */
  private readonly seatModel = signal<SeatCategoryFormValue>({
    name: '',
    price: 50,
    totalSeats: 100,
    seatingCapacity: 1,
  });

  /**
   * Angular Signal Form for the "Add Seat Category" modal.
   * Validators run reactively on each field. On submit, calls
   * SeatCategoryService.create() then reloads the event to show the new category.
   */


  //signal form

  readonly seatForm = form(
    this.seatModel,
    (path) => {

      //constraints / validations
      required(path.name, { message: 'Category name is required' });
      required(path.price, { message: 'Price is required' });
      min(path.price, 0, { message: 'Price cannot be negative' });
      required(path.totalSeats, { message: 'Total seats is required' });
      min(path.totalSeats, 1, { message: 'Total seats must be at least 1' });
      required(path.seatingCapacity, { message: 'Seating capacity is required' });
      min(path.seatingCapacity, 1, { message: 'Seating capacity must be at least 1' });
    },
    {
      submission: {
        //after submission
        action: async (field) => {
          const evt = this.event();
          // if event doesnt exist return
          if (!evt) return;
          try {
            this.submittingCategory.set(true);
            const val = field().value();
            const req: SeatCategoryCreateRequest = {
              name: val.name,
              price: val.price,
              totalSeats: val.totalSeats,
              seatingCapacity: val.seatingCapacity,
            };
            //Build the API request payload from those values.
            // POST /api/organizer/events/{id}/seat-categories
            await firstValueFrom(this.seatCategoryService.create(evt.id, req));
            this.showSeatModal.set(false);
            this.actionSuccess.set('Seat category added successfully!');
            this.loadEvent(evt.id); // refresh so new category appears in the ticket options grid
            return;
          } catch (err) {
            const msg = err instanceof ApiError ? err.message : 'Failed to create seat category.';
            return { kind: 'serverError', message: msg };
          } finally {
            this.submittingCategory.set(false);
          }
        },
      },
    },
  );



  constructor() {
    // Read the ':id' route param and kick off the event fetch
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadEvent(id);
    } else {
      this.error.set('Invalid event ID.');
      this.loading.set(false);
    }
  }

  /**
   * Fetches the event by ID.
   * Uses the appropriate endpoint based on the user's role:
   *   - Admin    → GET /api/admin/events/{id}     (sees all statuses)
   *   - Organizer → GET /api/organizer/events/{id} (sees own events in all statuses)
   *   - Public   → GET /api/events/{id}           (only PUBLISHED events)
   * Auto-selects the first seat category in the booking widget after load.
   */
  loadEvent(id: string): void {
    this.loading.set(true);
    this.error.set(null);

    //Pick the right endpoint based on role
    const fetch = this.authService.isAdmin()
      ? this.eventService.adminGetById(id)
      : this.authService.isOrganizer()
        ? this.eventService.getOrganizerById(id)
        : this.eventService.getPublicById(id);

    fetch.subscribe({
      //actually trigger the request
      next: (res) => {
        this.event.set(res);
        // Auto-select first seat category for the booking widget
        if (res.seatCategories && res.seatCategories.length > 0 && !this.selectedSeatCategoryId()) {
          this.selectedSeatCategoryId.set(res.seatCategories[0].id);
        }
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to load event details.');
        this.loading.set(false);
      },
    });
  }

  /** Called when the customer changes the seat-category dropdown. Resets quantity to 1. */
  onSeatCategoryChange(categoryId: string): void {
    this.selectedSeatCategoryId.set(categoryId);
    this.quantity.set(1);
    this.error.set(null);
  }

  /** Called when the customer clicks the +/- quantity stepper. */
  onQuantityChange(qty: number): void {
    this.quantity.set(qty);
    this.error.set(null);
  }

  /**
   * Client-side guard run before calling BookingService.reserve().
   * Validates: category selected, quantity >= 1, seats available,
   * quantity <= availableSeats, quantity <= seatingCapacity limit.
   * Returns false and sets the error signal if any check fails.
   */
  private isReservationValid(): boolean {
    const categoryId = this.selectedSeatCategoryId();
    const qty = this.quantity();

    if (!categoryId) {
      this.error.set('Please select a seat category');
      return false;
    }

    if (!Number.isInteger(qty) || qty < 1) {
      this.error.set('Quantity must be at least 1');
      return false;
    }

    const category = this.selectedCategory();
    if (!category) {
      this.error.set('Selected seat category not found');
      return false;
    }

    if (category.availableSeats <= 0) {
      this.error.set('No seats are available');
      return false;
    }

    if (qty > category.availableSeats) {
      this.error.set('Quantity exceeds available seats');
      return false;
    }

    if (category.seatingCapacity && qty > category.seatingCapacity) {
      this.error.set(`Quantity exceeds seat limit of ${category.seatingCapacity} per person`);
      return false;
    }

    return true;
  }

  /**
   * Submits the seat reservation.
   * Validates client-side first (isReservationValid), then calls
   * BookingService.reserve() which POSTs to /api/customer/bookings.
   * On success: navigates to /bookings (Person 3's view).
   * On 409 conflict: a concurrent booking took the last seat — reload
   *   the event so the UI reflects the updated availableSeats count.
   */
  reserve(): void {
    if (!this.isReservationValid()) {
      return;
    }

    const evt = this.event();
    if (!evt) return;

    this.reserving.set(true);
    this.error.set(null);

    this.bookingService
      .reserve({
        eventId: evt.id,
        seatCategoryId: this.selectedSeatCategoryId(),
        quantity: this.quantity(),
      })
      .subscribe({
        next: (booking) => {
          this.reserving.set(false);
          // Land on the booking so the customer can Confirm (that is what issues the ticket).
          this.router.navigate(['/customer/bookings', booking.id]);
        },
        error: (err) => {
          if (err instanceof ApiError && err.status === 409) {
            // Seat conflict — someone else took the seat(s) at the same time
            this.error.set(
              'The selected seats are no longer available. Available seat counts have been updated. Please select another quantity or seat category.',
            );
          } else {
            this.error.set(err instanceof ApiError ? err.message : 'Failed to reserve seats');
          }
          this.loadEvent(evt.id); // refresh live availableSeats after conflict
          this.reserving.set(false);
        },
      });
  }

  /**
   * Publishes the event (DRAFT → PUBLISHED).
   * POST /api/organizer/events/{id}/publish
   * Backend validates: ≥1 seat category with totalSeats>0, future dateTime.
   */
  publishEvent(): void {
    const evt = this.event();
    if (!evt) return;

    this.loading.set(true);
    this.eventService.publish(evt.id).subscribe({
      next: (updated) => {
        this.event.set(updated);
        this.actionSuccess.set(
          'Event published successfully! It is now visible in public browse listings.',
        );
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to publish event.');
        this.loading.set(false);
      },
    });
  }

  /**
   * Cancels the event (→ CANCELLED) with a confirmation dialog.
   * Admin uses adminCancel(), organizer uses organizerCancel().
   * The backend cascade-cancels all PENDING/CONFIRMED bookings.
   */
  cancelEvent(): void {
    const evt = this.event();
    if (!evt) return;

    if (
      !confirm(
        'Are you sure you want to cancel this event? This action will cascade-cancel active bookings.',
      )
    ) {
      return;
    }

    this.loading.set(true);
    // Admin can cancel any event; organizer only their own (enforced server-side too)
    const cancel$ = this.authService.isAdmin()
      ? this.eventService.adminCancel(evt.id)
      : this.eventService.organizerCancel(evt.id);

    cancel$.subscribe({
      next: (updated) => {
        this.event.set(updated);
        this.actionSuccess.set('Event has been cancelled.');
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to cancel event.');
        this.loading.set(false);
      },
    });
  }

  /** Opens the "Add Seat Category" modal. */
  openSeatModal(): void {
    this.showSeatModal.set(true);
  }

  /** Closes the "Add Seat Category" modal. */
  closeSeatModal(): void {
    this.showSeatModal.set(false);
  }

  /**
   * Returns the % of seats still available for a given category.
   * Used to set the width of the progress bar in the ticket options grid.
   */
  getAvailabilityPercent(sc: SeatCategorySummary): number {
    if (!sc.totalSeats) return 0;
    return Math.round((sc.availableSeats / sc.totalSeats) * 100);
  }

  /**
   * Toggles the attendees/bookings panel for organizer/admin.
   * Lazy-loads bookings on first open (avoids an unnecessary API call
   * when the organizer doesn't need to see attendees).
   */
  toggleBookingsPanel(): void {
    const evt = this.event();
    if (!evt) return;
    if (this.showBookings()) {
      this.showBookings.set(false);
      return;
    }
    this.showBookings.set(true);
    // Only fetch if we haven't loaded them yet
    if (this.bookings().length === 0) {
      this.loadEventBookings(evt.id);
    }
  }

  /**
   * Fetches all bookings for this event.
   * Organizer: GET /api/organizer/events/{id}/bookings
   * Admin:     GET /api/admin/bookings (filtered by eventId, handled by BookingService)
   */
  loadEventBookings(eventId: string): void {
    this.bookingsLoading.set(true);
    this.bookingsError.set(null);
    this.bookingService.getEventBookings(eventId).subscribe({
      next: (res: BookingResponse[]) => {
        this.bookings.set(res);
        this.bookingsLoading.set(false);
      },
      error: (err: unknown) => {
        this.bookingsError.set(err instanceof ApiError ? err.message : 'Failed to load bookings.');
        this.bookingsLoading.set(false);
      },
    });
  }

  /** Maps an EventCategory to its hero image path (used in the hero banner). */
  getCategoryImage(category: EventCategory): string {
    return getEventCategoryImage(category);
  }

  /** Hides broken <img> elements gracefully. */
  onImageError(event: Event): void {
    const target = event.target as HTMLImageElement;
    if (target) {
      target.style.display = 'none';
    }
  }
}
