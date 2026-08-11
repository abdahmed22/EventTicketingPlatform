// ─── Person 2 Model: event.model.ts ───────────────────────────────────────────
// Defines all TypeScript interfaces and types used across Event-related flows:
// public browse, organizer create/edit/publish, and admin event management.
// ──────────────────────────────────────────────────────────────────────────────
import { VenueSummary } from './venue.model';
import { SeatCategorySummary } from './seat-category.model';

/**
 * Fixed set of event categories.
 * Used as a filter in GET /api/events and as a form field when creating events.
 * Keep in sync with the backend EventCategory enum.
 */
export type EventCategory = 'MUSIC' | 'SPORTS' | 'CONFERENCE' | 'THEATRE' | 'OTHER';

/**
 * Lifecycle states of an event:
 *  - DRAFT      → created but not visible to the public yet
 *  - PUBLISHED  → visible to all; customers can browse and book
 *  - CANCELLED  → all active bookings are cascade-cancelled; no new bookings allowed
 */
export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED';

/**
 * Lightweight event shape returned in paginated list responses
 * (GET /api/events, GET /api/organizer/events, GET /api/admin/events).
 * Contains only scalar fields — venue and seat categories are NOT embedded here
 * to keep list payloads small. The detail view uses EventResponse instead.
 */
export interface EventSummary {
  id: string;
  title: string;
  description?: string;
  category: EventCategory;
  eventDate: string;   // ISO date string, e.g. "2025-11-30"
  eventTime: string;   // HH:mm string, e.g. "19:00"
  status: EventStatus;
  venueId: string;     // used to look up venue name in EventListComponent
  organizerId: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * Full event shape returned from the event detail endpoint
 * (GET /api/events/{id}, GET /api/organizer/events/{id}, GET /api/admin/events/{id}).
 * Embeds the full VenueSummary object and all SeatCategorySummary[] with live
 * availableSeats counts — used by EventDetailComponent to display the booking widget.
 */
export interface EventResponse {
  id: string;
  title: string;
  description?: string;
  category: EventCategory;
  eventDate: string;
  eventTime: string;
  status: EventStatus;
  venue: VenueSummary;                    // embedded venue info (name, address, capacity)
  organizerId: string;
  seatCategories: SeatCategorySummary[]; // live availability shown on the detail page
  createdAt: string;
  updatedAt: string;
}

/**
 * Request body for POST /api/organizer/events (create a new DRAFT event).
 * venueId must reference an APPROVED venue — the form dropdown is pre-filtered.
 * dateTime must be in the future (validated server-side too).
 */
export interface EventCreateRequest {
  title: string;
  description?: string;
  category: EventCategory;
  eventDate: string;  // ISO date string
  eventTime: string;  // HH:mm string
  venueId: string;    // must be an APPROVED venue ID
}

/**
 * Request body for PUT /api/organizer/events/{id} (update an existing event).
 * All fields are optional — only send what changed.
 * Changing the venue is allowed only while the event is in DRAFT.
 */
export interface EventUpdateRequest {
  title?: string;
  description?: string;
  category?: EventCategory;
  eventDate?: string;
  eventTime?: string;
  venueId?: string;
}

/**
 * Query-parameter shape consumed by EventService.browsePublished() and
 * related list methods. All fields are optional — omit to apply no filter.
 * Used by EventListComponent's sidebar filter panel.
 *
 * minPrice/maxPrice filter against SeatCategory prices on the backend.
 * status is only used in organizer/admin list calls (not public browse).
 */
export interface EventFilterParams {
  category?: EventCategory;  // filter by event category tab
  dateFrom?: string;         // ISO date — events on or after this date
  dateTo?: string;           // ISO date — events on or before this date
  minPrice?: number;         // minimum seat category price
  maxPrice?: number;         // maximum seat category price
  venueId?: string;          // filter to one specific venue
  organizerId?: string;      // filter to events owned by one organizer
  status?: EventStatus;      // organizer/admin only — filter by event status
  page?: number;             // 0-indexed page number for pagination
  size?: number;             // page size (default 9 in EventListComponent)
}
