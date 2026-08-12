import { Component, inject, signal } from '@angular/core';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { BookingService } from '../../../services/booking/booking.service';
import { TicketService } from '../../../services/ticket/ticket.service';
import { EventService } from '../../../services/event/event.service';
import { AuthService } from '../../../services/auth/auth.service';
import { BookingResponse } from '../../../models/booking.model';
import { CustomerTicket, ticketStatusLabel } from '../../../models/ticket.model';
import { EventResponse } from '../../../models/event.model';
import { ApiError } from '../../../models/api-error.model';

@Component({
  selector: 'app-booking-detail',
  standalone: true,
  imports: [DatePipe, CurrencyPipe, RouterLink],
  templateUrl: './booking-detail.component.html',
  styleUrl: './booking-detail.component.css'
})
export class BookingDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly bookingService = inject(BookingService);
  private readonly ticketService = inject(TicketService);
  private readonly eventService = inject(EventService);
  readonly authService = inject(AuthService);

  readonly ticketStatusLabel = ticketStatusLabel;

  readonly booking = signal<BookingResponse | null>(null);
  readonly tickets = signal<CustomerTicket[]>([]);
  readonly event = signal<EventResponse | null>(null);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly downloading = signal(false);
  readonly downloadError = signal<string | null>(null);
  readonly actioning = signal(false);

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set('Invalid booking.');
      this.loading.set(false);
      return;
    }
    this.load(id);
  }

  load(bookingId: string): void {
    const userId = this.authService.user()?.id;
    if (!userId) {
      this.error.set('You must be logged in to view this booking.');
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      bookings: this.bookingService.myBookings(),
      ticket: this.ticketService.getMyTicketForBooking(bookingId).pipe(catchError(() => of(null as CustomerTicket | null)))
    }).subscribe({
      next: ({ bookings, ticket }) => {
        const booking = bookings.find((b) => b.id === bookingId);
        if (!booking) {
          this.error.set('Booking not found among your bookings.');
          this.loading.set(false);
          return;
        }
        this.booking.set(booking);
        this.tickets.set(ticket ? [ticket] : []);

        this.eventService
          .getPublicById(booking.eventId)
          .pipe(catchError(() => of(null as EventResponse | null)))
          .subscribe({
            next: (event) => {
              this.event.set(event);
              this.loading.set(false);
            },
            error: () => {
              this.loading.set(false);
            }
          });
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to load booking.');
        this.loading.set(false);
      }
    });
  }

  confirmBooking(): void {
    const booking = this.booking();
    if (!booking || booking.status !== 'PENDING' || this.actioning()) {
      return;
    }
    this.actioning.set(true);
    this.error.set(null);
    this.bookingService.confirm(booking.id).subscribe({
      next: () => {
        this.actioning.set(false);
        this.load(booking.id);
      },
      error: (err: unknown) => {
        this.actioning.set(false);
        this.error.set(err instanceof ApiError ? err.message : 'Failed to confirm booking.');
        this.load(booking.id);
      }
    });
  }

  cancelBooking(): void {
    const booking = this.booking();
    if (!booking || this.actioning()) {
      return;
    }
    if (!window.confirm('Cancel this booking?')) {
      return;
    }
    this.actioning.set(true);
    this.error.set(null);
    this.bookingService.cancel(booking.id).subscribe({
      next: () => {
        this.actioning.set(false);
        this.load(booking.id);
      },
      error: (err: unknown) => {
        this.actioning.set(false);
        this.error.set(err instanceof ApiError ? err.message : 'Failed to cancel booking.');
        this.load(booking.id);
      }
    });
  }

  // Downloads the single PDF ticket for this booking (covers every seat in
  // the booking) and triggers a browser save/open of the file.
  downloadPdf(): void {
    const userId = this.authService.user()?.id;
    const bookingId = this.booking()?.id;
    if (!userId || !bookingId) {
      return;
    }

    this.downloading.set(true);
    this.downloadError.set(null);

    this.ticketService.downloadMyTicketPdf(bookingId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `ticket-${bookingId}.pdf`;
        link.click();
        URL.revokeObjectURL(url);
        this.downloading.set(false);
      },
      error: (err: unknown) => {
        this.downloadError.set(err instanceof ApiError ? err.message : 'Failed to download ticket PDF.');
        this.downloading.set(false);
      }
    });
  }
}
