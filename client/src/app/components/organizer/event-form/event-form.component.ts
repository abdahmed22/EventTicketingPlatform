// ─── Person 2 Component: event-form.component.ts ────────────────────────────
// Routes:
//   - '/organizer/events/new'      (create mode)
//   - '/organizer/events/:id/edit' (edit mode)
// A single component handles both create and update.
// Detects mode by checking whether ':id' is present in the route params.
// Key constraint: the venue dropdown only lists APPROVED venues.
// ──────────────────────────────────────────────────────────────────────────────
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { form, FormField, FormRoot, required } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import { EventService } from '../../../services/event/event.service';
import { VenueService } from '../../../services/venue/venue.service';
import { EventCategory, EventCreateRequest, EventUpdateRequest } from '../../../models/event.model';
import { VenueResponse } from '../../../models/venue.model';
import { ApiError } from '../../../models/api-error.model';

/** Shape of the reactive model used by the event Signal Form */
interface EventFormValue {
  title: string;
  description: string;
  category: EventCategory;
  eventDate: string;  // ISO date string
  eventTime: string;  // HH:mm string
  venueId: string;    // must be an APPROVED venue
}

/**
 * Person 2 Component: EventFormComponent
 *
 * Used for both creating and editing events (dual-mode component).
 *
 * CREATE mode ('/organizer/events/new'):
 *   - Form is pre-filled with sensible defaults.
 *   - On submit: POST /api/organizer/events → navigates to the new event's detail page.
 *
 * EDIT mode ('/organizer/events/:id/edit'):
 *   - Loads the existing event via getOrganizerById() (not the public endpoint,
 *     so DRAFT events work too) and pre-fills the form.
 *   - On submit: PUT /api/organizer/events/{id} → navigates back to the detail page.
 *
 * Important: the venue dropdown calls listApprovedVenues() so only APPROVED
 * venues are shown — a PENDING venue the organizer submitted cannot be used yet.
 */

@Component({
  selector: 'app-event-form',
  standalone: true,
  imports: [FormField, FormRoot, RouterLink],
  templateUrl: './event-form.component.html',
  styleUrl: './event-form.component.css'
})
export class EventFormComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventService = inject(EventService);
  private readonly venueService = inject(VenueService);

  readonly isEditMode = signal(false);            // true when ':id' is in the route
  readonly eventId = signal<string | null>(null); // the event being edited (null in create mode)
  readonly approvedVenues = signal<VenueResponse[]>([]); // venues available in the dropdown
  readonly loadingVenues = signal(true);

  /** Fixed enum values for the category select element */
  readonly categories: EventCategory[] = ['MUSIC', 'SPORTS', 'CONFERENCE', 'THEATRE', 'OTHER'];

  /** Reactive model that backs the Signal Form; updated in edit mode via loadEventForEdit() */
  private readonly eventModel = signal<EventFormValue>({
    title: '',
    description: '',
    category: 'MUSIC',
    eventDate: '',
    eventTime: '19:00', // sensible default
    venueId: ''
  });

  /**
   * Angular Signal Form definition.
   * required() validators provide inline error messages the template renders
   * next to each field when touched + invalid.
   *
   * Submission action:
   *   - Edit mode:   PUT /api/organizer/events/{id}
   *   - Create mode: POST /api/organizer/events
   * Both redirect to the event's detail page on success.
   * On server error (e.g. 409 past date, 400 validation), returns a serverError
   * which the Signal Form renders as a top-level form error.
   */
  readonly eventForm = form(
    this.eventModel,
    (path) => {
      required(path.title, { message: 'Title is required' });
      required(path.category, { message: 'Category is required' });
      required(path.eventDate, { message: 'Event date is required' });
      required(path.eventTime, { message: 'Event time is required' });
      required(path.venueId, { message: 'Select an approved venue' });
    },
    {
      submission: {
        action: async (field) => {
          try {
            const val = field().value();
            if (this.isEditMode() && this.eventId()) {
              // ─ EDIT mode ─────────────────────────────────────────────
              const req: EventUpdateRequest = {
                title: val.title,
                description: val.description.trim() || undefined,
                category: val.category,
                eventDate: val.eventDate,
                eventTime: val.eventTime,
                venueId: val.venueId
              };
              const res = await firstValueFrom(this.eventService.update(this.eventId()!, req));
              await this.router.navigate(['/events', res.id]);
            } else {
              // ─ CREATE mode ──────────────────────────────────────────
              const req: EventCreateRequest = {
                title: val.title,
                description: val.description.trim() || undefined,
                category: val.category,
                eventDate: val.eventDate,
                eventTime: val.eventTime,
                venueId: val.venueId
              };
              const res = await firstValueFrom(this.eventService.create(req));
              await this.router.navigate(['/events', res.id]);
            }
            return;
          } catch (err) {
            const message = err instanceof ApiError ? err.message : 'Failed to save event.';
            return { kind: 'serverError', message };
          }
        }
      }
    }
  );

  constructor() {
    // Always load approved venues first so the dropdown is ready
    this.loadApprovedVenues();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      // Edit mode: ':id' param is present
      this.isEditMode.set(true);
      this.eventId.set(id);
      this.loadEventForEdit(id);
    }
    // Create mode: no ':id' param, form starts with default values
  }

  /** Flag shown in the template to hint the organizer about pending venue requests */
  readonly hasPendingVenues = signal(false);

  /**
   * Loads APPROVED venues into the dropdown signal.
   * Also checks whether the organizer has any PENDING venue requests
   * (used to display a hint: "you have a venue pending admin approval").
   */
  loadApprovedVenues(): void {
    this.venueService.listApprovedVenues().subscribe({
      next: (venues) => {
        this.approvedVenues.set(venues);
        this.loadingVenues.set(false);
      },
      error: () => this.loadingVenues.set(false)
    });

    // Check if the organizer has submitted any venues that are still PENDING
    this.venueService.listMyVenues('PENDING').subscribe({
      next: (myPending) => {
        this.hasPendingVenues.set(myPending.length > 0);
      },
      error: () => {}
    });
  }

  /**
   * Fetches the event to pre-fill the form fields in edit mode.
   * Uses getOrganizerById() instead of the public endpoint because
   * DRAFT events are not returned by GET /api/events/{id} (public-only).
   */
  loadEventForEdit(id: string): void {
    this.eventService.getOrganizerById(id).subscribe({
      next: (evt) => {
        // Push existing values into the reactive model so the Signal Form renders them
        this.eventModel.set({
          title: evt.title,
          description: evt.description || '',
          category: evt.category,
          eventDate: evt.eventDate,
          eventTime: evt.eventTime,
          venueId: evt.venue.id
        });
      },
      error: () => {}
    });
  }
}
