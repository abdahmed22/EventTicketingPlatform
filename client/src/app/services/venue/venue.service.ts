// ─── Person 2 Service: venue.service.ts ─────────────────────────────────────────
// Wraps all HTTP calls for the Venue resource.
// Used by: EventFormComponent (venue dropdown), MyVenuesComponent, VenueReviewComponent.
// ──────────────────────────────────────────────────────────────────────────────
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { VenueCreateRequest, VenueResponse, VenueStatus, VenueUpdateRequest } from '../../models/venue.model';

/**
 * Person 2 Angular Service: VenueService
 *
 * Handles all API interactions for venues, grouped by role:
 *   - PUBLIC/ORGANIZER — fetching approved venues (for event-creation dropdowns)
 *   - ORGANIZER        — submitting and tracking their own venue requests
 *   - ADMIN            — full CRUD + approve/reject pending venue requests
 */
@Injectable({ providedIn: 'root' })
export class VenueService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  // ────────────────── PUBLIC / ORGANIZER ──────────────────

  /**
   * GET /api/events/venues
   * Returns all APPROVED venues as a flat list.
   * Used by EventListComponent (filter dropdown) and EventFormComponent
   * (venue picker — only APPROVED venues may be used when creating an event).
   */
  listApprovedVenues(): Observable<VenueResponse[]> {
    return this.http.get<VenueResponse[]>(`${this.apiUrl}/events/venues`);
  }

  // ────────────────── ORGANIZER ──────────────────

  /**
   * POST /api/organizer/venues
   * Submits a new venue registration request. The venue is created with
   * status PENDING and must be approved by an admin before it can be used
   * for event creation. Used by MyVenuesComponent's submission form.
   */
  submit(request: VenueCreateRequest): Observable<VenueResponse> {
    return this.http.post<VenueResponse>(`${this.apiUrl}/organizer/venues`, request);
  }

  /**
   * PUT /api/organizer/venues/{id}
   * Updates a venue that the organizer previously submitted.
   * Also used by MyVenuesComponent when Admin is editing a venue
   * (the component checks the role and routes to adminUpdate instead).
   */
  organizerUpdate(id: string, request: VenueCreateRequest): Observable<VenueResponse> {
    return this.http.put<VenueResponse>(`${this.apiUrl}/organizer/venues/${id}`, request);
  }

  /**
   * GET /api/organizer/venues[?status=]
   * Returns the organizer's own venue requests, optionally filtered by status.
   * Used by MyVenuesComponent to show the full list and by EventFormComponent
   * to check whether the organizer has any PENDING requests (info banner).
   */
  listMyVenues(status?: VenueStatus): Observable<VenueResponse[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<VenueResponse[]>(`${this.apiUrl}/organizer/venues`, { params });
  }

  // ────────────────── ADMIN ──────────────────

  /**
   * GET /api/admin/venues[?status=]
   * Returns all venues across all organizers, optionally filtered by status.
   * Used by VenueReviewComponent (admin review screen) and AdminDashboard
   * (for the combined PENDING queue panel — see §9.4 SRS).
   */
  listAdminVenues(status?: VenueStatus): Observable<VenueResponse[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<VenueResponse[]>(`${this.apiUrl}/admin/venues`, { params });
  }

  /**
   * POST /api/admin/venues
   * Admin creates a venue directly; it is auto-APPROVED (no review needed).
   * Used by MyVenuesComponent when the logged-in user is an Admin.
   */
  adminCreate(request: VenueCreateRequest): Observable<VenueResponse> {
    return this.http.post<VenueResponse>(`${this.apiUrl}/admin/venues`, request);
  }

  /** PUT /api/admin/venues/{id} — admin updates any venue */
  adminUpdate(id: string, request: VenueUpdateRequest): Observable<VenueResponse> {
    return this.http.put<VenueResponse>(`${this.apiUrl}/admin/venues/${id}`, request);
  }

  /** DELETE /api/admin/venues/{id} — admin hard-deletes a venue */
  adminDelete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/venues/${id}`);
  }

  /**
   * POST /api/admin/venues/{id}/approve
   * Transitions a PENDING venue to APPROVED.
   * Once approved, the venue appears in the event-creation dropdown for all organizers.
   * Used by VenueReviewComponent's approve button.
   */
  approve(id: string): Observable<VenueResponse> {
    return this.http.post<VenueResponse>(`${this.apiUrl}/admin/venues/${id}/approve`, {});
  }

  /**
   * POST /api/admin/venues/{id}/reject
   * Transitions a PENDING venue to REJECTED.
   * The venue row is kept as a permanent record; it can never be used for events.
   * An optional rejectionReason string can be sent in the body.
   */
  reject(id: string, reason?: string): Observable<VenueResponse> {
    return this.http.post<VenueResponse>(`${this.apiUrl}/admin/venues/${id}/reject`, { reason });
  }
}