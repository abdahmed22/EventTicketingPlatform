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

@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  // Public browse published events with filters
  browsePublished(filters: EventFilterParams = {}): Observable<PageResponse<EventSummary>> {
    let params = this.buildFilterParams(filters);
    return this.http.get<PageResponse<EventSummary>>(`${this.apiUrl}/events`, { params });
  }

  // Public event detail
  getPublicById(id: string): Observable<EventResponse> {
    return this.http.get<EventResponse>(`${this.apiUrl}/events/${id}`);
  }

  // Organizer: List own events (all statuses)
  listOrganizerEvents(filters: EventFilterParams = {}): Observable<PageResponse<EventSummary>> {
    let params = this.buildFilterParams(filters);
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    return this.http.get<PageResponse<EventSummary>>(`${this.apiUrl}/organizer/events`, { params });
  }

  // Organizer event detail (all statuses)
  getOrganizerById(id: string): Observable<EventResponse> {
    return this.http.get<EventResponse>(`${this.apiUrl}/organizer/events/${id}`);
  }

  // Organizer: Create event in DRAFT
  create(request: EventCreateRequest): Observable<EventResponse> {
    return this.http.post<EventResponse>(`${this.apiUrl}/organizer/events`, request);
  }

  // Organizer: Update event
  update(id: string, request: EventUpdateRequest): Observable<EventResponse> {
    return this.http.put<EventResponse>(`${this.apiUrl}/organizer/events/${id}`, request);
  }

  // Organizer: Publish event
  publish(id: string): Observable<EventResponse> {
    return this.http.post<EventResponse>(`${this.apiUrl}/organizer/events/${id}/publish`, {});
  }

  // Organizer: Cancel event
  organizerCancel(id: string): Observable<EventResponse> {
    return this.http.post<EventResponse>(`${this.apiUrl}/organizer/events/${id}/cancel`, {});
  }

  // Admin: List all events regardless of status
  adminList(filters: EventFilterParams = {}): Observable<PageResponse<EventSummary>> {
    let params = this.buildFilterParams(filters);
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    return this.http.get<PageResponse<EventSummary>>(`${this.apiUrl}/admin/events`, { params });
  }

  // Admin: Get detail
  adminGetById(id: string): Observable<EventResponse> {
    return this.http.get<EventResponse>(`${this.apiUrl}/admin/events/${id}`);
  }

  // Admin: Update event
  adminUpdate(id: string, request: Partial<EventUpdateRequest> & { status?: string }): Observable<EventResponse> {
    return this.http.put<EventResponse>(`${this.apiUrl}/admin/events/${id}`, request);
  }

  // Admin: Delete event
  adminDelete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/events/${id}`);
  }

  // Admin: Cancel event
  adminCancel(id: string): Observable<EventResponse> {
    return this.http.post<EventResponse>(`${this.apiUrl}/admin/events/${id}/cancel`, {});
  }

  private buildFilterParams(filters: EventFilterParams): HttpParams {
    let params = new HttpParams();
    if (filters.category) params = params.set('category', filters.category);
    if (filters.dateFrom) params = params.set('dateFrom', filters.dateFrom);
    if (filters.dateTo) params = params.set('dateTo', filters.dateTo);
    if (filters.minPrice !== undefined && filters.minPrice !== null) params = params.set('minPrice', filters.minPrice.toString());
    if (filters.maxPrice !== undefined && filters.maxPrice !== null) params = params.set('maxPrice', filters.maxPrice.toString());
    if (filters.venueId) params = params.set('venueId', filters.venueId);
    if (filters.organizerId) params = params.set('organizerId', filters.organizerId);
    if (filters.page !== undefined) params = params.set('page', filters.page.toString());
    if (filters.size !== undefined) params = params.set('size', filters.size.toString());
    return params;
  }
}
