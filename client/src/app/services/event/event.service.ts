// ─── Person 2 Service: event.service.ts ─────────────────────────────────────────
// All HTTP calls related to the Event entity live here.
// Methods are grouped by role so it is easy to see who can call what.
// Every method returns an Observable — components subscribe to it and
// update their signals in the next/error callbacks.
// ──────────────────────────────────────────────────────────────────────────────
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../../models/page.model';
import {
  EventCreateRequest,
  EventFilterParams,
  EventResponse,
  EventSummary,
  EventUpdateRequest
} from '../../models/event.model';

/**
 * Person 2 Angular Service: EventService
 *
 * Wraps all HTTP interactions for the Event resource.
 * Organised into three sections:
 *   - PUBLIC   — no auth token needed, only returns PUBLISHED events
 *   - ORGANIZER — requires JWT with role ORGANIZER (or ADMIN)
 *   - ADMIN    — requires JWT with role ADMIN, operates on all events
 *
 * Injected via Angular's DI into EventListComponent, EventDetailComponent,
 * EventFormComponent, MyEventsComponent, and the admin events screen.
 */
@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  // ─────────────────── PUBLIC ───────────────────

  /**
   * GET /api/events
   * Returns a paginated list of PUBLISHED events matching the given filters.
   * No auth token required — accessible by anyone, including anonymous users.
   * Used by EventListComponent to populate the browse catalog.
   */


  // Calling browsePublished() doesn't actually make the HTTP request yet —
  // it just returns a "recipe" for how to make it.
  // The request only fires when something calls .subscribe() on it, e.g.:
  browsePublished(filters: EventFilterParams = {}): Observable<PageResponse<EventSummary>> {
    let params = this.buildFilterParams(filters);
    return this.http.get<PageResponse<EventSummary>>(`${this.apiUrl}/events`, { params });
  }

  /**
   * GET /api/events/{id}
   * Returns full event detail (venue + seat categories) for a PUBLISHED event.
   * Used by EventDetailComponent for the public / customer view.
   * EventDetailComponent switches to getOrganizerById or adminGetById for
   * organizers/admins who need to see DRAFT or CANCELLED events.
   */
  getPublicById(id: string): Observable<EventResponse> {
    return this.http.get<EventResponse>(`${this.apiUrl}/events/${id}`);
  }

  // ────────────────── ORGANIZER ──────────────────

  /**
   * GET /api/organizer/events
   * Returns all events owned by the logged-in organizer, in all statuses
   * (DRAFT, PUBLISHED, CANCELLED). Used by MyEventsComponent.
   * Status filter is added as a query param when not 'ALL'.
   */
  listOrganizerEvents(filters: EventFilterParams = {}): Observable<PageResponse<EventSummary>> {
    let params = this.buildFilterParams(filters);
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    return this.http.get<PageResponse<EventSummary>>(`${this.apiUrl}/organizer/events`, { params });
  }

  /**
   * GET /api/organizer/events/{id}
   * Returns full event detail for any event owned by the organizer,
   * regardless of status. Used by EventDetailComponent (organizer view)
   * and EventFormComponent when loading an event for editing.
   */
  getOrganizerById(id: string): Observable<EventResponse> {
    return this.http.get<EventResponse>(`${this.apiUrl}/organizer/events/${id}`);
  }

  /**
   * POST /api/organizer/events
   * Creates a new event with status DRAFT.
   * The request must include an APPROVED venueId — the form pre-filters venues.
   */
  create(request: EventCreateRequest): Observable<EventResponse> {
    return this.http.post<EventResponse>(`${this.apiUrl}/organizer/events`, request);
  }

  /**
   * PUT /api/organizer/events/{id}
   * Updates an existing event. Can be used on DRAFT or PUBLISHED events.
   * The backend validates dateTime is still in the future.
   */
  update(id: string, request: EventUpdateRequest): Observable<EventResponse> {
    return this.http.put<EventResponse>(`${this.apiUrl}/organizer/events/${id}`, request);
  }

  /**
   * POST /api/organizer/events/{id}/publish
   * Transitions the event from DRAFT → PUBLISHED.
   * Backend validates: at least one seat category with totalSeats > 0, future dateTime.
   * After publish, the event appears in public GET /api/events listings.
   */
  publish(id: string): Observable<EventResponse> {
    return this.http.post<EventResponse>(`${this.apiUrl}/organizer/events/${id}/publish`, {});
  }

  /**
   * POST /api/organizer/events/{id}/cancel (organizer path)
   * Transitions the event to CANCELED. The backend cascade-cancels all
   * PENDING and CONFIRMED bookings for that event.
   */
  organizerCancel(id: string): Observable<EventResponse> {
    return this.http.post<EventResponse>(`${this.apiUrl}/organizer/events/${id}/cancel`, {});
  }

  // ─────────────────── ADMIN ───────────────────

  /**
   * GET /api/admin/events
   * Returns ALL events regardless of status and organizer.
   * Admins have full visibility with no ownership restriction (§4.4 SRS).
   */
  adminList(filters: EventFilterParams = {}): Observable<PageResponse<EventSummary>> {
    let params = this.buildFilterParams(filters);
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    return this.http.get<PageResponse<EventSummary>>(`${this.apiUrl}/admin/events`, { params });
  }

  /** GET /api/admin/events/{id} — admin full event detail */
  adminGetById(id: string): Observable<EventResponse> {
    return this.http.get<EventResponse>(`${this.apiUrl}/admin/events/${id}`);
  }

  /** PUT /api/admin/events/{id} — admin update any event field */
  adminUpdate(
    id: string,
    request: Partial<EventUpdateRequest> & { status?: string },
  ): Observable<EventResponse> {
    return this.http.put<EventResponse>(`${this.apiUrl}/admin/events/${id}`, request);
  }

  /** DELETE /api/admin/events/{id} — hard delete (admin only) */
  adminDelete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/events/${id}`);
  }

  /**
   * POST /api/admin/events/{id}/cancel
   * Same cascade-cancel behaviour as the organizer path but available
   * on any event regardless of who owns it.
   */
  adminCancel(id: string): Observable<EventResponse> {
    return this.http.post<EventResponse>(`${this.apiUrl}/admin/events/${id}/cancel`, {});
  }

  // ────────────────── HELPERS ──────────────────

  /**
   * Converts an EventFilterParams object into an HttpParams instance.
   * Only appends a param if the value is defined and non-empty.
   * Used by browsePublished(), listOrganizerEvents(), and adminList().
   */
  private buildFilterParams(filters: EventFilterParams): HttpParams {
    let params = new HttpParams();
    if (filters.category) params = params.set('category', filters.category);
    if (filters.dateFrom) params = params.set('dateFrom', filters.dateFrom);
    if (filters.dateTo) params = params.set('dateTo', filters.dateTo);
    if (filters.minPrice !== undefined && filters.minPrice !== null)
      params = params.set('minPrice', filters.minPrice.toString());
    if (filters.maxPrice !== undefined && filters.maxPrice !== null)
      params = params.set('maxPrice', filters.maxPrice.toString());
    if (filters.venueId) params = params.set('venueId', filters.venueId);
    if (filters.organizerId) params = params.set('organizerId', filters.organizerId);
    if (filters.page !== undefined) params = params.set('page', filters.page.toString());
    if (filters.size !== undefined) params = params.set('size', filters.size.toString());
    return params;
  }
}
