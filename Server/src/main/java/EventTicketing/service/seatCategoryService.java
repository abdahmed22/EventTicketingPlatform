package EventTicketing.service;

import EventTicketing.dto.seatCategoryDto;
import EventTicketing.exception.ForbiddenActionException;
import EventTicketing.exception.ResourceNotFoundException;
import EventTicketing.model.Event;
import EventTicketing.model.SeatCategory;
import EventTicketing.model.User;
import EventTicketing.model.enums.UserRole;
import EventTicketing.repository.eventRepository;
import EventTicketing.repository.seatCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class seatCategoryService {

    private final seatCategoryRepository seatCategoryRepository;
    private final eventRepository eventRepository;

    public List<seatCategoryDto.Summary> listByEvent(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        return seatCategoryRepository.findByEvent(event).stream()
                .map(seatCategoryDto.Summary::from)
                .toList();
    }

    @Transactional
    public seatCategoryDto.Response create(UUID eventId, User organizer, seatCategoryDto.CreateRequest request) {
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

        return seatCategoryDto.Response.from(seatCategoryRepository.save(seatCategory));
    }

    @Transactional
    public seatCategoryDto.Response update(UUID seatCategoryId, User organizer, seatCategoryDto.UpdateRequest request) {
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

        return seatCategoryDto.Response.from(seatCategoryRepository.save(seatCategory));
    }
}
