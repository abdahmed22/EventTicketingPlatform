import {
  Component,
  computed,
  inject,
  OnDestroy,
  OnInit,
  signal,
} from '@angular/core';

import { DatePipe, CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription, interval } from 'rxjs';

import { BookingService } from '../../../services/booking/booking.service';
import { TicketService } from '../../../services/ticket/ticket.service';
import { AuthService } from '../../../services/auth/auth.service';

import {
  BookingResponse,
  BookingStatus,
} from '../../../models/booking.model';

import { ApiError } from '../../../models/api-error.model';

export type BookingFilterOption = 'ALL' | 'CONFIRMED' | 'PENDING' | 'CANCELLED';

@Component({
  selector: 'app-my-bookings',
  imports: [DatePipe, CurrencyPipe, RouterLink],
  templateUrl: './my-bookings.component.html',
  styleUrl: './my-bookings.component.css',
})
export class MyBookingsComponent implements OnInit, OnDestroy {

  private readonly bookingService = inject(BookingService);
  private readonly ticketService = inject(TicketService);
  private readonly authService = inject(AuthService);

  private timerSubscription?: Subscription;

  readonly bookings = signal<BookingResponse[]>([]);

  readonly loading = signal<boolean>(false);

  readonly error = signal<string | null>(null);

  readonly actioningId = signal<string | null>(null);

  readonly downloadingId = signal<string | null>(null);

  readonly remainingTimes =
    signal<Record<string, string>>({});

  readonly activeFilter = signal<BookingFilterOption>('ALL');

  readonly filterOptions: { label: string; value: BookingFilterOption }[] = [
    { label: 'All', value: 'ALL' },
    { label: 'Confirmed', value: 'CONFIRMED' },
    { label: 'Pending Payment', value: 'PENDING' },
    { label: 'Canceled', value: 'CANCELLED' }
  ];

  readonly filteredBookings = computed(() => {
    const filter = this.activeFilter();
    const list = this.bookings();
    if (filter === 'ALL') {
      return list;
    }
    if (filter === 'CONFIRMED') {
      return list.filter((b) => b.status === 'CONFIRMED');
    }
    if (filter === 'PENDING') {
      return list.filter((b) => b.status === 'PENDING');
    }
    if (filter === 'CANCELLED') {
      return list.filter((b) => b.status === 'CANCELLED' || b.status === 'EXPIRED');
    }
    return list;
  });

  setFilter(filter: BookingFilterOption): void {
    this.activeFilter.set(filter);
  }


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

    this.bookingService.myBookings().subscribe({

      next: (bookings: BookingResponse[]) => {

        this.bookings.set(bookings);

        this.updateCountdowns();

        this.loading.set(false);
      },

      error: (err: unknown) => {

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

        // Immediately update local booking status to EXPIRED so UI updates without delay
        this.bookings.update((list) =>
          list.map((b) => (b.id === booking.id ? { ...b, status: 'EXPIRED' as BookingStatus } : b))
        );

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


  // Downloads the single PDF ticket issued for a confirmed booking
  // (covers every seat in that booking).
  downloadTicket(
    bookingId: string
  ): void {

    if (this.downloadingId()) {
      return;
    }

    const userId = this.authService.user()?.id;

    if (!userId) {
      this.error.set('You must be logged in to download your ticket.');
      return;
    }

    this.downloadingId.set(bookingId);
    this.error.set(null);

    this.ticketService
      .downloadTicketPdf(userId, bookingId)
      .subscribe({

        next: (blob) => {

          const url = URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `ticket-${bookingId}.pdf`;
          link.click();
          URL.revokeObjectURL(url);

          this.downloadingId.set(null);
        },

        error: (err: unknown) => {

          this.error.set(
            err instanceof ApiError
              ? err.message
              : 'Failed to download ticket PDF'
          );

          this.downloadingId.set(null);
        },
      });
  }
}