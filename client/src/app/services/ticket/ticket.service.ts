import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AdminTicket,
  CustomerTicket,
  OrganizerEventTicket
} from '../../models/ticket.model';

@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  // Customer: list all of the logged-in customer's tickets across events.
  // Backend route: GET /api/tickets/customer/{customerId}
  listCustomerTickets(customerId: string): Observable<CustomerTicket[]> {
    return this.http.get<CustomerTicket[]>(`${this.apiUrl}/tickets/customer/${customerId}`);
  }

  // Organizer: list tickets issued for one of the organizer's own events.
  // Backend route: GET /api/tickets/organizer/{event_uuid}
  listOrganizerEventTickets(eventId: string): Observable<OrganizerEventTicket[]> {
    return this.http.get<OrganizerEventTicket[]>(`${this.apiUrl}/tickets/organizer/${eventId}`);
  }

  // Organizer: mark an attendee as checked in for an event.
  // Backend route: POST /api/tickets/organizer/events/{eventId}/check-in
  checkIn(eventId: string, ticketCode: string): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/tickets/organizer/events/${eventId}/check-in`, {
      ticketCode
    });
  }

  // Admin: list all tickets issued for a given event.
  // Backend route: GET /api/tickets/admin/EventTickets/{eventUUID}
  listAdminEventTickets(eventId: string): Observable<AdminTicket[]> {
    return this.http.get<AdminTicket[]>(`${this.apiUrl}/tickets/admin/EventTickets/${eventId}`);
  }

  // Admin: look up a single ticket by its UUID.
  // Backend route: GET /api/tickets/admin/{uuid}
  getAdminTicket(ticketUuid: string): Observable<AdminTicket> {
    return this.http.get<AdminTicket>(`${this.apiUrl}/tickets/admin/${ticketUuid}`);
  }

  // Admin: void a ticket.
  // Backend route: POST /api/tickets/admin/{ticketId}/cancel
  cancelAdminTicket(ticketId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/tickets/admin/${ticketId}/cancel`, {});
  }
}
