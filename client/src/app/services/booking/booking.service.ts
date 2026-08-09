import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from '../../../environments/environment';
import {
  BookingResponse,
  CreateBookingRequest,
} from '../../models/booking.model';

@Injectable({
  providedIn: 'root',
})
export class BookingService {

  private readonly http = inject(HttpClient);

  private readonly apiUrl = `${environment.apiUrl}/bookings`;

  reserve(request: CreateBookingRequest) {
    return this.http.post<BookingResponse>(
      this.apiUrl,
      request
    );
  }

  confirm(bookingId: string) {
    return this.http.post<BookingResponse>(
      `${this.apiUrl}/${bookingId}/confirm`,
      {}
    );
  }

  cancel(bookingId: string) {
    return this.http.post<BookingResponse>(
      `${this.apiUrl}/${bookingId}/cancel`,
      {}
    );
  }

  getMyBookings() {
    return this.http.get<BookingResponse[]>(
      `${this.apiUrl}/my`
    );
  }
}

