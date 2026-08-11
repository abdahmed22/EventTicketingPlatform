// ─── Person 2 Service: seat-category.service.ts ──────────────────────────────
// Wraps HTTP calls for creating and updating SeatCategory records.
// Person 3 (Booking) relies on SeatCategorySummary (from EventResponse)
// but does NOT use this service — booking/availability is handled server-side.
// ──────────────────────────────────────────────────────────────────────────────
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  SeatCategoryCreateRequest,
  SeatCategoryResponse,
  SeatCategorySummary,
  SeatCategoryUpdateRequest
} from '../../models/seat-category.model';

/**
 * Person 2 Angular Service: SeatCategoryService
 *
 * Provides two write operations for seat categories:
 *   - create(): called from the "Add Seat Category" modal in EventDetailComponent
 *   - update(): called from the edit seat category form (organizer view)
 *
 * Reading seat categories is done via EventService (they are embedded in EventResponse),
 * not through a dedicated endpoint, so there is no listByEvent here.
 */
@Injectable({ providedIn: 'root' })
export class SeatCategoryService {
  //inject httpclient / requests
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /**
   * GET /api/events/{eventId}/seat-categories
   * Returns all seat categories for a specific event.
   * Note: In practice, seat categories are embedded inside EventResponse.seatCategories,
   * so this endpoint may not be called directly — it is here as a fallback.
   */
  listByEvent(eventId: string): Observable<SeatCategorySummary[]> {
    return this.http.get<SeatCategorySummary[]>(`${this.apiUrl}/events/${eventId}/seat-categories`);
  }

  /**
   * POST /api/organizer/events/{eventId}/seat-categories
   * Creates a new seat category for an event the organizer owns.
   * Backend sets availableSeats = totalSeats automatically on creation.
   * Called from the "Add Seat Category" modal in EventDetailComponent.
   */
  create(eventId: string, request: SeatCategoryCreateRequest): Observable<SeatCategoryResponse> {
    return this.http.post<SeatCategoryResponse>(`${this.apiUrl}/organizer/events/${eventId}/seat-categories`, request);
  }

  /**
   * PUT /api/organizer/seat-categories/{id}
   * Updates an existing seat category (name, price, totalSeats, seatingCapacity).
   * Increasing totalSeats also increases availableSeats by the same delta.
   * Decreasing totalSeats below the count of current bookings returns 409.
   */
  update(id: string, request: SeatCategoryUpdateRequest): Observable<SeatCategoryResponse> {
    return this.http.put<SeatCategoryResponse>(`${this.apiUrl}/organizer/seat-categories/${id}`, request);
  }
}
