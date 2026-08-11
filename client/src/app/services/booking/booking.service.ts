import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BookingResponse } from '../../models/booking.model';

@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  // Customer: reserve seats (creates a PENDING booking).
  // Backend route: POST /api/bookings
  // The request shape is inlined so this service doesn't depend on a shared
  // booking model type that may be owned by another part of the project.
  reserve(request: { eventId: string; seatCategoryId: string; quantity: number }): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(`${this.apiUrl}/bookings`, request);
  }

  // Customer: confirm a PENDING booking (tickets are issued on confirmation).
  // Backend route: POST /api/bookings/{id}/confirm
  confirm(id: string): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(`${this.apiUrl}/bookings/${id}/confirm`, {});
  }

  // Customer: cancel a booking.
  // Backend route: POST /api/bookings/{id}/cancel
  cancel(id: string): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(`${this.apiUrl}/bookings/${id}/cancel`, {});
  }

  // Customer: list the logged-in customer's own bookings.
  // Backend route: GET /api/bookings/my
  myBookings(): Observable<BookingResponse[]> {
    return this.http.get<BookingResponse[]>(`${this.apiUrl}/bookings/my`);
  }

  // Organizer: list bookings for one of their own events.
  // Backend route: GET /api/organizer/events/{eventId}/bookings
  getEventBookings(eventId: string): Observable<BookingResponse[]> {
    return this.http.get<BookingResponse[]>(`${this.apiUrl}/organizer/events/${eventId}/bookings`);
  }
}