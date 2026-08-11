package EventTicketing.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public abstract class DashboardDTO {

    public record PendingOrganizerApplication(
            UUID id,
            String name,
            String email,
            String phone,
            String organizationName,
            String reason,
            Instant submittedAt) {
    }

    public record PendingVenue(
            UUID id,
            String name,
            String address,
            Integer capacity,
            UUID requestedById,
            String requestedByName) {
    }

    public record Summary(
            int pendingOrganizerApplications,
            List<PendingOrganizerApplication> organizerApplications,
            int pendingVenueRequests,
            List<PendingVenue> venueRequests) {
    }
}