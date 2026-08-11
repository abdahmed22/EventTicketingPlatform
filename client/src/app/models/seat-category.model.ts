// ─── Person 2 Model: seat-category.model.ts ───────────────────────────────────
// Interfaces for SeatCategory, which defines pricing tiers within an event.
// Each event can have multiple seat categories (e.g. VIP, Standard).
// availableSeats is the field protected by pessimistic locking on the backend
// (Person 3's responsibility — §7 of the SRS).
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Lightweight seat category shape embedded inside EventResponse.seatCategories.
 * Shown in the booking widget on EventDetailComponent.
 * availableSeats reflects live seat counts — always fetch fresh from the server
 * before booking to avoid stale UI counts.
 */
export interface SeatCategorySummary {
  id: string;
  name: string;            // e.g. "VIP", "Standard"
  price: number;           // price per ticket (BigDecimal on backend)
  totalSeats: number;      // physical seats allocated for this category
  availableSeats: number;  // live count; protected by pessimistic locking (§7)
  seatingCapacity: number; // how many people can sit in one ticket (default 1)
}

/**
 * Full seat category shape returned from the organizer/admin seat category endpoints.
 * Extends SeatCategorySummary with the linked eventId and venueId FKs,
 * used in organizer views to identify which event/venue a category belongs to.
 */
export interface SeatCategoryResponse {
  id: string;
  eventId: string;         // the event this category belongs to
  venueId: string;         // the venue of that event
  name: string;
  price: number;
  totalSeats: number;
  availableSeats: number;
  seatingCapacity: number;
}

/**
 * Request body for POST /api/organizer/events/{eventId}/seat-categories.
 * When created, availableSeats is set equal to totalSeats on the backend.
 * seatingCapacity defaults to 1 (standard single-person seat).
 */
export interface SeatCategoryCreateRequest {
  name: string;
  price: number;           // must be ≥ 0
  totalSeats: number;      // must be > 0
  seatingCapacity: number; // must be ≥ 1; how many people fit per ticket
}

/**
 * Request body for PUT /api/organizer/seat-categories/{id}.
 * All fields are optional (partial update).
 * Backend rule: decreasing totalSeats below the current booking count is rejected
 * with a 409 to protect existing reservations.
 */
export interface SeatCategoryUpdateRequest {
  name?: string;
  price?: number;
  totalSeats?: number;
  seatingCapacity?: number;
}
