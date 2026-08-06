package EventTicketing.dto;

import EventTicketing.model.SeatCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class seatCategoryDto {

    public record CreateRequest(
            @NotBlank(message = "Seat category name is required") String name,
            @NotNull(message = "Price is required") @Min(value = 0, message = "Price cannot be negative") BigDecimal price,
            @NotNull(message = "Total seats is required") @Min(value = 1, message = "Total seats must be greater than 0") Integer totalSeats,
            @NotNull(message = "Seating capacity is required") @Min(value = 1, message = "Seating capacity must be at least 1") Integer seatingCapacity) {
    }

    public record UpdateRequest(
            String name,
            BigDecimal price,
            @Min(value = 1, message = "Total seats must be greater than 0") Integer totalSeats,
            @Min(value = 1, message = "Seating capacity must be at least 1") Integer seatingCapacity) {
    }

    public record Response(
            UUID id,
            UUID eventId,
            UUID venueId,
            String name,
            BigDecimal price,
            Integer totalSeats,
            Integer availableSeats,
            Integer seatingCapacity) {
        public static Response from(SeatCategory seatCategory) {
            return new Response(
                    seatCategory.getId(),
                    seatCategory.getEvent().getId(),
                    seatCategory.getVenue().getId(),
                    seatCategory.getName(),
                    seatCategory.getPrice(),
                    seatCategory.getTotalSeats(),
                    seatCategory.getAvailableSeats(),
                    seatCategory.getSeatingCapacity());
        }
    }

    public record Summary(
            UUID id,
            String name,
            BigDecimal price,
            Integer totalSeats,
            Integer availableSeats,
            Integer seatingCapacity) {
        public static Summary from(SeatCategory seatCategory) {
            return new Summary(
                    seatCategory.getId(),
                    seatCategory.getName(),
                    seatCategory.getPrice(),
                    seatCategory.getTotalSeats(),
                    seatCategory.getAvailableSeats(),
                    seatCategory.getSeatingCapacity());
        }
    }
}
