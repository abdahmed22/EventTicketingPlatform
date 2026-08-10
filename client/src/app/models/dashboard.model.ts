/**
 * GET /api/admin/dashboard
 *
 * Returns both pending review queues in a single payload so the admin review
 * dashboard can render the organizer applications and venue requests side by
 * side on one screen (SRS 6.C).
 */
export interface PendingOrganizerApplication {
  id: string;
  name: string;
  email: string;
  phone: string;
  organizationName: string | null;
  reason: string | null;
  submittedAt: string;
}

export interface PendingVenue {
  id: string;
  name: string;
  address: string;
  capacity: number;
  submittedById: string | null;
  submittedByName: string | null;
}

export interface DashboardSummary {
  pendingOrganizerApplications: number;
  organizerApplications: PendingOrganizerApplication[];
  pendingVenueRequests: number;
  venueRequests: PendingVenue[];
}
