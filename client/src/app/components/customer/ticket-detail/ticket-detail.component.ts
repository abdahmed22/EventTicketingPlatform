import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
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

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [DatePipe, CurrencyPipe, RouterLink],
  templateUrl: './ticket-detail.component.html',
  styleUrl: './ticket-detail.component.css'
})
export class TicketDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly ticketService = inject(TicketService);
  private readonly bookingService = inject(BookingService);
  private readonly eventService = inject(EventService);
  readonly authService = inject(AuthService);

  readonly ticketStatusLabel = ticketStatusLabel;

  readonly ticket = signal<CustomerTicket | null>(null);
  readonly booking = signal<BookingResponse | null>(null);
  readonly event = signal<EventResponse | null>(null);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly downloading = signal(false);
  readonly downloadError = signal<string | null>(null);

  readonly seatCategoryName = computed<string>(() => {
    const booking = this.booking();
    if (booking?.seatCategoryName) {
      return booking.seatCategoryName;
    }
    const ticket = this.ticket();
    const event = this.event();
    if (ticket && event) {
      const found = event.seatCategories.find((category) => category.id === ticket.seat);
      if (found) {
        return found.name;
      }
    }
    return '—';
  });

  constructor() {
    const ticketCode = this.route.snapshot.paramMap.get('ticketCode');
    if (!ticketCode) {
      this.error.set('Invalid ticket.');
      this.loading.set(false);
      return;
    }
    this.load(ticketCode);
  }

  load(ticketCode: string): void {
    if (!this.authService.user()?.id) {
      this.error.set('You must be logged in to view this ticket.');
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      tickets: this.ticketService.listMyTickets(),
      bookings: this.bookingService.myBookings().pipe(catchError(() => of([] as BookingResponse[])))
    }).subscribe({
      next: ({ tickets, bookings }) => {
        const ticket = tickets.find((item) => item.ticketCode === ticketCode);
        if (!ticket) {
          this.error.set('Ticket not found among your tickets.');
          this.loading.set(false);
          return;
        }
        this.ticket.set(ticket);
        this.booking.set(bookings.find((b) => b.id === ticket.bookingId) ?? null);

        this.eventService
          .getPublicById(ticket.evnt)
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
        this.error.set(err instanceof ApiError ? err.message : 'Failed to load ticket.');
        this.loading.set(false);
      }
    });
  }

  downloadPdf(): void {
    const bookingId = this.ticket()?.bookingId;
    if (!bookingId) {
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
