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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatCategoryService {

    private final SeatCategoryRepository seatCategoryRepository;
    private final EventRepository eventRepository;

    public List<SeatCategoryDto.Summary> listByEvent(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        return seatCategoryRepository.findByEvent(event).stream()
                .map(SeatCategoryDto.Summary::from)
                .toList();
    }

    @Transactional
    public SeatCategoryDto.Response create(UUID eventId, User organizer, SeatCategoryDto.CreateRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        if (!event.getOrganizer().getId().equals(organizer.getId()) && organizer.getRole() != UserRole.ADMIN) {
            throw new ForbiddenActionException("You do not have permission to add seat categories to this event.");
        }
        if (event.getStatus() == Event.Status.CANCELLED) {
            throw new ForbiddenActionException("Cannot add seat categories to a cancelled event.");
        }

        SeatCategory seatCategory = SeatCategory.builder()
                .event(event)
                .venue(event.getVenue())
                .name(request.name())
                .price(request.price())
                .totalSeats(request.totalSeats())
                .availableSeats(request.totalSeats())
                .seatingCapacity(request.seatingCapacity())
                .build();

        return SeatCategoryDto.Response.from(seatCategoryRepository.save(seatCategory));
    }

    @Transactional
    public SeatCategoryDto.Response update(UUID seatCategoryId, User organizer, SeatCategoryDto.UpdateRequest request) {
        SeatCategory seatCategory = seatCategoryRepository.findById(seatCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat category not found with id: " + seatCategoryId));
        Event event = seatCategory.getEvent();

        if (!event.getOrganizer().getId().equals(organizer.getId()) && organizer.getRole() != UserRole.ADMIN) {
            throw new ForbiddenActionException("You do not have permission to update this seat category.");
        }
        if (event.getStatus() == Event.Status.CANCELLED) {
            throw new ForbiddenActionException("Cannot update seat categories for a cancelled event.");
        }

        if (request.name() != null) {
            seatCategory.setName(request.name());
        }
        if (request.price() != null) {
            seatCategory.setPrice(request.price());
        }
        if (request.seatingCapacity() != null) {
            seatCategory.setSeatingCapacity(request.seatingCapacity());
        }
        if (request.totalSeats() != null) {
            int currentTotal = seatCategory.getTotalSeats();
            int currentAvailable = seatCategory.getAvailableSeats();
            int reservedSeats = currentTotal - currentAvailable;
            int newTotal = request.totalSeats();

            if (newTotal < reservedSeats) {
                throw new ForbiddenActionException("Total seats cannot be reduced below already reserved seats.");
            }

            int delta = newTotal - currentTotal;
            seatCategory.setTotalSeats(newTotal);
            seatCategory.setAvailableSeats(currentAvailable + delta);
        }

        return SeatCategoryDto.Response.from(seatCategoryRepository.save(seatCategory));
    }
}
