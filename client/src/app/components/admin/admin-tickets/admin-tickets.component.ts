import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TicketService } from '../../../services/ticket/ticket.service';
import { EventService } from '../../../services/event/event.service';
import { UserAdminService } from '../../../services/user-admin/user-admin.service';
import { AdminTicket, ticketStatusLabel } from '../../../models/ticket.model';
import { EventResponse, EventSummary } from '../../../models/event.model';
import { UserResponse } from '../../../models/user.model';
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
  private readonly userAdminService = inject(UserAdminService);

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
  readonly downloadingId = signal<string | null>(null);

  // Cache of userOwnerUUID -> user, so ticket owners are shown by name
  // instead of raw UUIDs. Shared across the event-browse table and the
  // single-ticket lookup panel.
  readonly owners = signal<Map<string, UserResponse>>(new Map());

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
        this.loadOwnerNames(tickets.map((t) => t.userOwnerUUID));
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to load tickets for this event.');
        this.loadingTickets.set(false);
      }
    });
  }

  // Fetches and caches display names for any owner UUIDs not already
  // cached. Best-effort: a failed lookup just falls back to showing the
  // raw UUID for that ticket instead of blocking the whole table.
  private loadOwnerNames(ownerIds: string[]): void {
    const cache = this.owners();
    const missing = [...new Set(ownerIds)].filter((id) => id && !cache.has(id));

    if (missing.length === 0) {
      return;
    }

    forkJoin(
      missing.map((id) =>
        this.userAdminService.getUser(id).pipe(catchError(() => of(null as UserResponse | null)))
      )
    ).subscribe((users) => {
      const next = new Map(this.owners());
      users.forEach((user, index) => {
        if (user) {
          next.set(missing[index], user);
        }
      });
      this.owners.set(next);
    });
  }

  ownerName(ticket: AdminTicket): string {
    return this.owners().get(ticket.userOwnerUUID)?.name ?? ticket.userOwnerUUID;
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

  // Admins can download any ticket's PDF (backend allows owner OR admin).
  downloadTicket(ticket: AdminTicket): void {
    if (this.downloadingId()) {
      return;
    }

    this.downloadingId.set(ticket.uuid);
    this.error.set(null);

    this.ticketService.downloadTicketPdf(ticket.userOwnerUUID, ticket.bookingId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `ticket-${ticket.bookingId}.pdf`;
        link.click();
        URL.revokeObjectURL(url);
        this.downloadingId.set(null);
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to download ticket PDF.');
        this.downloadingId.set(null);
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
        this.loadOwnerNames([ticket.userOwnerUUID]);
      },
      error: (err: unknown) => {
        this.lookupError.set(err instanceof ApiError ? err.message : 'Ticket not found.');
        this.lookingUp.set(false);
      }
    });
  }
}
