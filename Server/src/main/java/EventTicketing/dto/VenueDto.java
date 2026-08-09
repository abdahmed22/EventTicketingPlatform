package EventTicketing.dto;

import EventTicketing.model.Venue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public class VenueDto {

    public record CreateRequest(
            @NotBlank(message = "Venue name is required") String name,
            @NotBlank(message = "Venue address is required") String address,
            @NotNull(message = "Capacity is required") @Min(value = 1, message = "Capacity must be greater than 0") Integer capacity) {
    }

    public record RejectRequest(
            String reason) {
    }

    public record Response(
            UUID id,
            String name,
            String address,
            Integer capacity,
            Venue.Status status,
            LocalDateTime reviewedAt,
            UUID reviewedBy,
            UUID requestedBy) {
        public static Response from(Venue venue) {
            return new Response(
                    venue.getId(),
                    venue.getName(),
                    venue.getAddress(),
                    venue.getCapacity(),
                    venue.getStatus(),
                    venue.getReviewedAt(),
                    venue.getReviewedBy() == null ? null : venue.getReviewedBy().getId(),
                    venue.getRequestedBy() == null ? null : venue.getRequestedBy().getId());
        }
    }
}
