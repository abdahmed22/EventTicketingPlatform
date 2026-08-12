import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TicketService } from '../../../services/ticket/ticket.service';
import { BookingService } from '../../../services/booking/booking.service';
import { EventService } from '../../../services/event/event.service';
import { AuthService } from '../../../services/auth/auth.service';
import { CustomerTicket, ticketStatusLabel } from '../../../models/ticket.model';
import { BookingResponse } from '../../../models/booking.model';
import { EventResponse } from '../../../models/event.model';
import { ApiError } from '../../../models/api-error.model';

interface CustomerTicketView {
  ticket: CustomerTicket;
  booking?: BookingResponse;
  event?: EventResponse;
}

@Component({
  selector: 'app-my-tickets',
  standalone: true,
  imports: [DatePipe, CurrencyPipe, RouterLink],
  templateUrl: './my-tickets.component.html',
  styleUrl: './my-tickets.component.css'
})
export class MyTicketsComponent {
  private readonly ticketService = inject(TicketService);
  private readonly bookingService = inject(BookingService);
  private readonly eventService = inject(EventService);
  readonly authService = inject(AuthService);

  readonly ticketStatusLabel = ticketStatusLabel;

  private readonly tickets = signal<CustomerTicket[]>([]);
  private readonly bookingsMap = signal<Map<string, BookingResponse>>(new Map());
  private readonly eventsMap = signal<Map<string, EventResponse>>(new Map());

  readonly views = computed<CustomerTicketView[]>(() => {
    const bookings = this.bookingsMap();
    const events = this.eventsMap();
    return this.tickets().map((ticket) => ({
      ticket,
      booking: bookings.get(ticket.bookingId),
      event: events.get(ticket.evnt)
    }));
  });

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly downloadingBookingId = signal<string | null>(null);
  readonly downloadError = signal<string | null>(null);

  constructor() {
    this.load();
  }

  load(): void {
    const userId = this.authService.user()?.id;
    if (!userId) {
      this.error.set('You must be logged in to view your tickets.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      tickets: this.ticketService.listMyTickets(),
      // Bookings may be empty for new customers; tolerate a failure gracefully.
      bookings: this.bookingService.myBookings().pipe(catchError(() => of([] as BookingResponse[])))
    }).subscribe({
      next: ({ tickets, bookings }) => {
        const eventIds = [...new Set(tickets.map((t) => t.evnt))];
        this.bookingsMap.set(new Map(bookings.map((b) => [b.id, b])));

        if (eventIds.length === 0) {
          this.eventsMap.set(new Map());
          this.tickets.set(tickets);
          this.loading.set(false);
          return;
        }

        forkJoin(
          eventIds.map((id) =>
            this.eventService.getPublicById(id).pipe(catchError(() => of(null as EventResponse | null)))
          )
        ).subscribe({
          next: (events) => {
            const eventsMap = new Map<string, EventResponse>();
            events.forEach((event) => {
              if (event) {
                eventsMap.set(event.id, event);
              }
            });
            this.eventsMap.set(eventsMap);
            this.tickets.set(tickets);
            this.loading.set(false);
          },
          error: (err: unknown) => {
            this.error.set(err instanceof ApiError ? err.message : 'Failed to load ticket details.');
            this.loading.set(false);
          }
        });
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to load your tickets.');
        this.loading.set(false);
      }
    });
  }

  seatCategoryName(view: CustomerTicketView): string {
    if (view.booking?.seatCategoryName) {
      return view.booking.seatCategoryName;
    }
    const seat = view.ticket.seat;
    const found = view.event?.seatCategories.find((category) => category.id === seat);
    return found?.name ?? '—';
  }

  // Downloads the PDF for a ticket's booking (covers every seat in it).
  downloadPdf(ticket: CustomerTicket): void {
    const userId = this.authService.user()?.id;
    if (!userId) {
      return;
    }

    this.downloadingBookingId.set(ticket.bookingId);
    this.downloadError.set(null);

    this.ticketService.downloadMyTicketPdf(ticket.bookingId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `ticket-${ticket.bookingId}.pdf`;
        link.click();
        URL.revokeObjectURL(url);
        this.downloadingBookingId.set(null);
      },
      error: (err: unknown) => {
        this.downloadError.set(err instanceof ApiError ? err.message : 'Failed to download ticket PDF.');
        this.downloadingBookingId.set(null);
      }
    });
  }
}
