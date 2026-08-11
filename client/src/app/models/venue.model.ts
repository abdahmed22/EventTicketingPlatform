// ─── Person 2 Model: venue.model.ts ───────────────────────────────────────────
// Defines all interfaces and types for the Venue entity.
// Venues go through an admin review cycle (PENDING → APPROVED/REJECTED).
// Only APPROVED venues can be used when creating an event.
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Admin review status for a venue registration request.
 *  - PENDING   → submitted by an organizer, awaiting admin review
 *  - APPROVED  → usable by any organizer when creating an event
 *  - REJECTED  → denied by admin; kept as a record, never usable
 *
 * Note: Admin-created venues skip review and are auto-APPROVED.
 */
export type VenueStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

/**
 * Lightweight venue shape embedded inside EventResponse.
 * Does NOT include status or review fields — only the info
 * needed to describe where an event takes place.
 */
export interface VenueSummary {
  id: string;
  name: string;
  address: string;
  capacity: number;
}

/**
 * Full venue shape returned from organizer/admin venue endpoints
 * (GET /api/organizer/venues, GET /api/admin/venues).
 * Includes admin review metadata so the organizer can see
 * whether their request was approved, rejected, or is still pending.
 */
export interface VenueResponse {
  id: string;
  name: string;
  address: string;
  capacity: number;
  status: VenueStatus;   // current review state
  reviewedAt?: string;   // when the admin actioned this (nullable)
  reviewedBy?: string;   // ID of the admin who reviewed it (nullable)
  requestedBy?: string;  // ID of the organizer who submitted it
}

/**
 * Request body for:
 *  - POST /api/organizer/venues (organizer submits a new venue → status = PENDING)
 *  - POST /api/admin/venues     (admin creates a venue directly → status = APPROVED)
 */
export interface VenueCreateRequest {
  name: string;
  address: string;
  capacity: number; // total physical seat count; must be > 0
}

/**
 * Request body for PUT /api/admin/venues/{id} or PUT /api/organizer/venues/{id}.
 * All three fields are required (full replacement, not partial patch).
 */
export interface VenueUpdateRequest {
  name: string;
  address: string;
  capacity: number;
}
