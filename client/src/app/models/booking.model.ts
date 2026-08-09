export type BookingStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'CANCELLED'
  | 'EXPIRED';

export interface CreateBookingRequest {
  eventId: string;
  seatCategoryId: string;
  quantity: number;
}

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
  confirmedAt: string | null;
  cancelledAt: string | null;
}