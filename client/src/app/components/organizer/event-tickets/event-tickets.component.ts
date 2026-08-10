import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { TicketService } from '../../../services/ticket/ticket.service';
import { EventService } from '../../../services/event/event.service';
import { OrganizerEventTicket, ticketStatusLabel } from '../../../models/ticket.model';
import { EventResponse, EventSummary } from '../../../models/event.model';
import { ApiError } from '../../../models/api-error.model';

@Component({
  selector: 'app-event-tickets',
  standalone: true,
  imports: [DatePipe, CurrencyPipe],
  templateUrl: './event-tickets.component.html',
  styleUrl: './event-tickets.component.css'
})
export class EventTicketsComponent {
  private readonly ticketService = inject(TicketService);
  private readonly eventService = inject(EventService);

  readonly ticketStatusLabel = ticketStatusLabel;

  readonly events = signal<EventSummary[]>([]);
  readonly selectedEventId = signal<string>('');
  readonly selectedEvent = signal<EventResponse | null>(null);
  readonly tickets = signal<OrganizerEventTicket[]>([]);

  readonly loadingEvents = signal(false);
  readonly loadingTickets = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  readonly actioningCode = signal<string | null>(null);

  readonly selectedEventTitle = computed(() => {
    const id = this.selectedEventId();
    return this.events().find((event) => event.id === id)?.title ?? '';
  });

  constructor() {
    this.loadEvents();
  }

  loadEvents(): void {
    this.loadingEvents.set(true);
    this.error.set(null);

    this.eventService.listOrganizerEvents({ size: 100 }).subscribe({
      next: (res) => {
        this.events.set(res.content);
        this.loadingEvents.set(false);
        if (res.content.length > 0) {
          this.selectEvent(res.content[0].id);
        }
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to load your events.');
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

    this.eventService.getOrganizerById(eventId).subscribe({
      next: (event) => this.selectedEvent.set(event),
      error: () => {
        /* venue/seat enrichment is best-effort; tickets still load below */
      }
    });

    this.ticketService.listOrganizerEventTickets(eventId).subscribe({
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

  seatCategoryName(ticket: OrganizerEventTicket): string {
    const found = this.selectedEvent()?.seatCategories.find((category) => category.id === ticket.seat);
    return found?.name ?? '—';
  }

  checkIn(ticket: OrganizerEventTicket): void {
    const eventId = this.selectedEventId();
    if (!eventId) {
      return;
    }
    this.actioningCode.set(ticket.ticketCode);
    this.error.set(null);

    this.ticketService.checkIn(eventId, ticket.ticketCode).subscribe({
      next: () => {
        this.actioningCode.set(null);
        this.success.set('Attendee checked in successfully.');
        this.loadEventTickets(eventId);
      },
      error: (err: unknown) => {
        this.actioningCode.set(null);
        this.error.set(err instanceof ApiError ? err.message : 'Failed to check in attendee.');
      }
    });
  }
}
