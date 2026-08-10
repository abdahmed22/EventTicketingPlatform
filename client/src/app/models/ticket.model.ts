export type TicketStatus = 'ISSUED' | 'CHECKED_IN' | 'CANCELLED';

/**
 * Map the backend TicketStatus enum to the customer-facing wording used in the
 * SRS (e.g. ISSUED => Active, CANCELLED => Voided) while keeping the raw value
 * for API calls and CSS classes.
 */
export function ticketStatusLabel(status: TicketStatus): string {
  switch (status) {
    case 'ISSUED':
      return 'Active';
    case 'CHECKED_IN':
      return 'Checked In';
    case 'CANCELLED':
      return 'Voided';
    default:
      return status;
  }
}

/**
 * GET /api/tickets/customer/{customer}?customer={customer}
 *
 * NOTE: the backend ticket payloads only carry UUIDs for the related event,
 * venue and seat category. The frontend enriches them with the public event
 * detail and the customer's own bookings to render readable information.
 */
export interface CustomerTicket {
  ticketCode: string;
  createdAt: string;
  bookingId: string;
  seat: string;
  evnt: string;
  venue: string;
  userOwnerUUID: string;
  totalPrice: number;
  status: TicketStatus;
}

/** GET /api/tickets/organizer/{event_uuid} */
export interface OrganizerEventTicket {
  ticketCode: string;
  createdAt: string;
  seat: string;
  venue: string;
  userOwnerUUID: string;
  totalPrice: number;
  status: TicketStatus;
}

/** GET /api/tickets/admin/{uuid} and /api/tickets/admin/EventTickets/{eventUUID} */
export interface AdminTicket {
  uuid: string;
  ticketCode: string;
  createdAt: string;
  bookingId: string;
  seat: string;
  evnt: string;
  venue: string;
  userOwnerUUID: string;
  totalPrice: number;
  status: TicketStatus;
}

/** POST /api/tickets/organizer/events/{eventId}/check-in */
export interface CheckInRequest {
  ticketCode: string;
}
