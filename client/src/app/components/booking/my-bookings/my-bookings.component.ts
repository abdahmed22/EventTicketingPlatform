import {
  Component,
  inject,
  OnDestroy,
  OnInit,
  signal,
} from '@angular/core';

import { DatePipe, CurrencyPipe } from '@angular/common';
import { Subscription, interval } from 'rxjs';

import { BookingService } from '../../../services/booking/booking.service';

import {
  BookingResponse,
  BookingStatus,
} from '../../../models/booking.model';

import { ApiError } from '../../../models/api-error.model';

@Component({
  selector: 'app-my-bookings',
  imports: [DatePipe, CurrencyPipe],
  templateUrl: './my-bookings.component.html',
  styleUrl: './my-bookings.component.css',
})
export class MyBookingsComponent implements OnInit, OnDestroy {

  private readonly bookingService = inject(BookingService);

  private timerSubscription?: Subscription;

  readonly bookings = signal<BookingResponse[]>([]);

  readonly loading = signal<boolean>(false);

  readonly error = signal<string | null>(null);

  readonly actioningId = signal<string | null>(null);

  readonly remainingTimes =
    signal<Record<string, string>>({});


  ngOnInit(): void {
    this.loadBookings();
    this.startCountdown();
  }


  ngOnDestroy(): void {
    this.timerSubscription?.unsubscribe();
  }


  private loadBookings(): void {

    this.loading.set(true);
    this.error.set(null);

    this.bookingService.getMyBookings().subscribe({

      next: (bookings) => {

        this.bookings.set(bookings);

        this.updateCountdowns();

        this.loading.set(false);
      },

      error: (err) => {

        this.error.set(
          err instanceof ApiError
            ? err.message
            : 'Failed to load bookings'
        );

        this.loading.set(false);
      },
    });
  }


  private startCountdown(): void {

    this.timerSubscription =
      interval(1000).subscribe(() => {

        this.updateCountdowns();

      });
  }


  private updateCountdowns(): void {

    const updatedTimes: Record<string, string> = {};

    let shouldRefreshBookings = false;


    this.bookings().forEach((booking) => {

      if (booking.status !== 'PENDING') {
        return;
      }


      const remainingSeconds =
        this.calculateRemainingSeconds(
          booking.expiresAt
        );


      if (remainingSeconds <= 0) {

        updatedTimes[booking.id] = '00:00';

        shouldRefreshBookings = true;

        return;
      }


      updatedTimes[booking.id] =
        this.formatTime(remainingSeconds);

    });


    this.remainingTimes.set(updatedTimes);


    /*
     * Backend is authoritative.
     *
     * When the countdown reaches zero,
     * reload bookings from the backend
     * instead of changing the status locally.
     */
    if (shouldRefreshBookings && !this.loading()) {

      this.loadBookings();
    }
  }


  private calculateRemainingSeconds(
    expiresAt: string
  ): number {

    const expiryTime =
      new Date(expiresAt).getTime();

    const currentTime =
      Date.now();

    const difference =
      Math.floor(
        (expiryTime - currentTime) / 1000
      );

    return Math.max(0, difference);
  }


  private formatTime(
    totalSeconds: number
  ): string {

    const minutes =
      Math.floor(totalSeconds / 60);

    const seconds =
      totalSeconds % 60;

    return `${this.pad(minutes)}:${this.pad(seconds)}`;
  }


  private pad(value: number): string {

    return value
      .toString()
      .padStart(2, '0');
  }


  confirmBooking(
    bookingId: string
  ): void {

    if (this.actioningId()) {
      return;
    }


    const booking =
      this.bookings().find(
        (item) => item.id === bookingId
      );


    if (
      !booking ||
      booking.status !== 'PENDING'
    ) {
      return;
    }


    this.actioningId.set(bookingId);
    this.error.set(null);


    this.bookingService
      .confirm(bookingId)
      .subscribe({

        next: (updatedBooking) => {

          this.bookings.update((bookings) =>
            bookings.map((booking) =>
              booking.id === updatedBooking.id
                ? updatedBooking
                : booking
            )
          );


          this.actioningId.set(null);

          this.updateCountdowns();
        },


        error: (err) => {

          this.error.set(
            err instanceof ApiError
              ? err.message
              : 'Failed to confirm booking'
          );


          this.actioningId.set(null);

          this.loadBookings();
        },
      });
  }


  cancelBooking(
    bookingId: string
  ): void {

    if (this.actioningId()) {
      return;
    }


    const booking =
      this.bookings().find(
        (item) => item.id === bookingId
      );


    if (
      !booking ||
      (booking.status !== 'PENDING' &&
        booking.status !== 'CONFIRMED')
    ) {
      return;
    }


    const confirmed =
      window.confirm(
        'Are you sure you want to cancel this booking?'
      );


    if (!confirmed) {
      return;
    }


    this.actioningId.set(bookingId);
    this.error.set(null);


    this.bookingService
      .cancel(bookingId)
      .subscribe({

        next: (updatedBooking) => {

          this.bookings.update((bookings) =>
            bookings.map((booking) =>
              booking.id === updatedBooking.id
                ? updatedBooking
                : booking
            )
          );


          this.actioningId.set(null);

          this.updateCountdowns();
        },


        error: (err) => {

          this.error.set(
            err instanceof ApiError
              ? err.message
              : 'Failed to cancel booking'
          );


          this.actioningId.set(null);

          this.loadBookings();
        },
      });
  }
}