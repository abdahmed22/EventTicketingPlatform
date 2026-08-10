export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED';

export interface BookingCreateRequest {
  eventId: string;
  seatCategoryId: string;
  quantity: number;
}

/**
 * GET /api/bookings/my (and the reserve/confirm/cancel responses).
 *
 * Unlike the ticket payloads, this DTO already carries the resolved event name
 * and seat category name, so it is used to enrich the customer ticket views.
 */
export interface BookingResponse {
  id: string;
  eventId: string;
  eventName: string;
  seatCategoryId: string;
  seatCategoryName: string;
  quantity: number;
  totalPrice: number;
  status: BookingStatus;
  createdAt: string;
  expiresAt: string;
}
