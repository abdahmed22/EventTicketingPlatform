import { Component, inject, signal } from '@angular/core';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { BookingService } from '../../../services/booking/booking.service';
import { BookingResponse } from '../../../models/booking.model';
import { ApiError } from '../../../models/api-error.model';

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [DatePipe, CurrencyPipe, RouterLink],
  templateUrl: './my-bookings.component.html',
  styleUrl: './my-bookings.component.css'
})
export class MyBookingsComponent {
  private readonly bookingService = inject(BookingService);

  readonly bookings = signal<BookingResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);

    this.bookingService.myBookings().subscribe({
      next: (bookings) => {
        this.bookings.set(bookings);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to load your bookings.');
        this.loading.set(false);
      }
    });
  }
}
