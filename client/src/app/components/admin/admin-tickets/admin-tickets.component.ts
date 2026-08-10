import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { TicketService } from '../../../services/ticket/ticket.service';
import { EventService } from '../../../services/event/event.service';
import { AdminTicket, ticketStatusLabel } from '../../../models/ticket.model';
import { EventResponse, EventSummary } from '../../../models/event.model';
import { ApiError } from '../../../models/api-error.model';

@Component({
  selector: 'app-admin-tickets',
  standalone: true,
  imports: [CurrencyPipe],
  templateUrl: './admin-tickets.component.html',
  styleUrl: './admin-tickets.component.css'
})
export class AdminTicketsComponent {
  private readonly ticketService = inject(TicketService);
  private readonly eventService = inject(EventService);

  readonly ticketStatusLabel = ticketStatusLabel;

  readonly events = signal<EventSummary[]>([]);
  readonly selectedEventId = signal<string>('');
  readonly selectedEvent = signal<EventResponse | null>(null);
  readonly tickets = signal<AdminTicket[]>([]);

  readonly loadingEvents = signal(false);
  readonly loadingTickets = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  readonly actioningId = signal<string | null>(null);

  // Single-ticket lookup by UUID
  readonly lookupUuid = signal('');
  readonly lookupTicket = signal<AdminTicket | null>(null);
  readonly lookingUp = signal(false);
  readonly lookupError = signal<string | null>(null);

  constructor() {
    this.loadEvents();
  }

  loadEvents(): void {
    this.loadingEvents.set(true);
    this.error.set(null);

    this.eventService.adminList({ size: 200 }).subscribe({
      next: (res) => {
        this.events.set(res.content);
        this.loadingEvents.set(false);
        if (res.content.length > 0) {
          this.selectEvent(res.content[0].id);
        }
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to load events.');
        this.loadingEvents.set(false);
      }
    });
  }

  selectEvent(eventId: string): void {
    this.selectedEventId.set(eventId);
    this.error.set(null);
    this.success.set(null);
    this.tickets.set([]);
    this.selectedEvent.set(null);
    this.loadEventTickets(eventId);
  }

  loadEventTickets(eventId: string): void {
    this.loadingTickets.set(true);

    this.eventService.adminGetById(eventId).subscribe({
      next: (event) => this.selectedEvent.set(event),
      error: () => {
        /* enrichment is best-effort */
      }
    });

    this.ticketService.listAdminEventTickets(eventId).subscribe({
      next: (tickets) => {
        this.tickets.set(tickets);
        this.loadingTickets.set(false);
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to load tickets for this event.');
        this.loadingTickets.set(false);
      }
    });
  }

  seatCategoryName(ticket: AdminTicket): string {
    const found = this.selectedEvent()?.seatCategories.find((category) => category.id === ticket.seat);
    return found?.name ?? '—';
  }

  venueName(ticket: AdminTicket): string {
    const event = this.selectedEvent();
    if (event && event.venue.id === ticket.venue) {
      return event.venue.name;
    }
    return '—';
  }

  cancelTicket(ticket: AdminTicket): void {
    this.actioningId.set(ticket.uuid);
    this.error.set(null);

    this.ticketService.cancelAdminTicket(ticket.uuid).subscribe({
      next: () => {
        this.actioningId.set(null);
        this.success.set(`Ticket ${ticket.ticketCode} was voided.`);
        const eventId = this.selectedEventId();
        if (eventId) {
          this.loadEventTickets(eventId);
        }
      },
      error: (err: unknown) => {
        this.actioningId.set(null);
        this.error.set(err instanceof ApiError ? err.message : 'Failed to void ticket.');
      }
    });
  }

  runLookup(): void {
    const uuid = this.lookupUuid().trim();
    if (!uuid) {
      return;
    }
    this.lookingUp.set(true);
    this.lookupError.set(null);
    this.lookupTicket.set(null);

    this.ticketService.getAdminTicket(uuid).subscribe({
      next: (ticket) => {
        this.lookupTicket.set(ticket);
        this.lookingUp.set(false);
      },
      error: (err: unknown) => {
        this.lookupError.set(err instanceof ApiError ? err.message : 'Ticket not found.');
        this.lookingUp.set(false);
      }
    });
  }
}
