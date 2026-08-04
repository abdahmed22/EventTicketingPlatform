package EventTicketing.dto;

import EventTicketing.model.Booking;
import EventTicketing.model.enums.BookingStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public abstract class bookingDto {

    public record CreateRequest(

            @NotNull(message = "Event id is required")
            UUID eventId,

            @NotNull(message = "Seat category id is required")
            UUID seatCategoryId,

            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be at least 1")
            Integer quantity

    ) {}

    public record Response(
            UUID id,
            UUID eventId,
            String eventName,
            UUID seatCategoryId,
            String seatCategoryName,
            Integer quantity,
            BigDecimal totalPrice,
            BookingStatus status,
            Instant createdAt,
            Instant expiresAt
    ) {

        public static Response from(Booking booking) {
            return new Response(
                    booking.getId(),
                    booking.getEvent().getId(),
                    booking.getEvent().getTitle(),
                    booking.getSeatCategory().getId(),
                    booking.getSeatCategory().getName(),
                    booking.getQuantity(),
                    booking.getTotalPrice(),
                    booking.getStatus(),
                    booking.getCreatedAt(),
                    booking.getExpiresAt()
            );
        }
    }
}