package EventTicketing.service;

import EventTicketing.dto.SeatCategoryDto;
import EventTicketing.exception.ForbiddenActionException;
import EventTicketing.exception.ResourceNotFoundException;
import EventTicketing.model.Event;
import EventTicketing.model.SeatCategory;
import EventTicketing.model.User;
import EventTicketing.model.enums.UserRole;
import EventTicketing.repository.EventRepository;
import EventTicketing.repository.SeatCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Person 2 Service: SeatCategoryService
 * Handles adding, updating, and listing seat categories for events.
 * Ensures totalSeats changes properly update availableSeats delta.
 */
@Service
@RequiredArgsConstructor
// Class-level default: all methods are read-only transactions unless overridden
// with @Transactional on the individual method (see create/update below).
@Transactional(readOnly = true)
public class SeatCategoryService {

    // Repository for persisting/querying SeatCategory entities.
    private final SeatCategoryRepository seatCategoryRepository;
    // Repository for looking up the parent Event an operation applies to.
    private final EventRepository eventRepository;

    /**
     * Lists all seat categories belonging to a given event.
     * Read-only operation (inherits the class-level @Transactional(readOnly = true)).
     */
    public List<SeatCategoryDto.Summary> listByEvent(UUID eventId) {
        // Fetch the event first so we can 404 early if it doesn't exist.
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        // Pull all seat categories tied to this event and map each entity
        // to a lightweight summary DTO for the response.
        return seatCategoryRepository.findByEvent(event).stream()
                .map(SeatCategoryDto.Summary::from)
                .toList();
    }

    /**
     * Creates a new seat category for an event.
     * Only the event's organizer or an ADMIN may perform this action,
     * and it's blocked once the event has been cancelled.
     */
    @Transactional // Overrides the class default to allow writes in this method.
    public SeatCategoryDto.Response create(UUID eventId, User organizer, SeatCategoryDto.CreateRequest request) {
        // Look up the target event; fail fast if it doesn't exist.
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        // Authorization check: caller must own the event or be an admin.
        if (!event.getOrganizer().getId().equals(organizer.getId()) && organizer.getRole() != UserRole.ADMIN) {
            throw new ForbiddenActionException("You do not have permission to add seat categories to this event.");
        }
        // Business rule: can't modify seating for a cancelled event.
        if (event.getStatus() == Event.Status.CANCELLED) {
            throw new ForbiddenActionException("Cannot add seat categories to a cancelled event.");
        }

        // Build the new SeatCategory. Note availableSeats starts out equal to
        // totalSeats since no seats have been reserved yet.
        SeatCategory seatCategory = SeatCategory.builder()
                .event(event)
                .venue(event.getVenue())
                .name(request.name())
                .price(request.price())
                .totalSeats(request.totalSeats())
                .availableSeats(request.totalSeats())
                .seatingCapacity(request.seatingCapacity())
                .build();

        // Persist and return the response DTO built from the saved entity.
        return SeatCategoryDto.Response.from(seatCategoryRepository.save(seatCategory));
    }

    /**
     * Updates an existing seat category. Supports partial updates: any field
     * left null in the request is left unchanged. Special handling is applied
     * to totalSeats so that availableSeats stays consistent with reservations
     * already made against this category.
     */
    @Transactional // Overrides the class default to allow writes in this method.
    public SeatCategoryDto.Response update(UUID seatCategoryId, User organizer, SeatCategoryDto.UpdateRequest request) {
        // Look up the seat category; fail fast if it doesn't exist.
        SeatCategory seatCategory = seatCategoryRepository.findById(seatCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat category not found with id: " + seatCategoryId));
        // The owning event is needed for both the authorization and status checks below.
        Event event = seatCategory.getEvent();

        // Authorization check: caller must own the parent event or be an admin.
        if (!event.getOrganizer().getId().equals(organizer.getId()) && organizer.getRole() != UserRole.ADMIN) {
            throw new ForbiddenActionException("You do not have permission to update this seat category.");
        }
        // Business rule: can't modify seating for a cancelled event.
        if (event.getStatus() == Event.Status.CANCELLED) {
            throw new ForbiddenActionException("Cannot update seat categories for a cancelled event.");
        }

        // Partial update: only overwrite fields that were actually supplied.
        if (request.name() != null) {
            seatCategory.setName(request.name());
        }
        if (request.price() != null) {
            seatCategory.setPrice(request.price());
        }
        if (request.seatingCapacity() != null) {
            seatCategory.setSeatingCapacity(request.seatingCapacity());
        }

        // totalSeats requires special handling because availableSeats must be
        // kept consistent: increasing/decreasing total seats should shift
        // availableSeats by the same delta, without touching seats already
        // reserved (totalSeats - availableSeats = reservedSeats).
        if (request.totalSeats() != null) {
            int currentTotal = seatCategory.getTotalSeats();
            int currentAvailable = seatCategory.getAvailableSeats();
            // Seats already booked/reserved out of the current total.
            int reservedSeats = currentTotal - currentAvailable;
            int newTotal = request.totalSeats();

            // Guard: never allow shrinking totalSeats below what's already reserved,
            // since that would make availableSeats negative.
            if (newTotal < reservedSeats) {
                throw new ForbiddenActionException("Total seats cannot be reduced below already reserved seats.");
            }

            // Apply the same change (delta) to availableSeats as to totalSeats,
            // so reservedSeats (the difference) stays the same.
            int delta = newTotal - currentTotal;
            seatCategory.setTotalSeats(newTotal);
            seatCategory.setAvailableSeats(currentAvailable + delta);
        }

        // Persist and return the response DTO built from the updated entity.
        return SeatCategoryDto.Response.from(seatCategoryRepository.save(seatCategory));
    }
}