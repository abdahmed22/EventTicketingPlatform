package EventTicketing.service;

import EventTicketing.dto.EventDto;
import EventTicketing.dto.PageResponse;
import EventTicketing.dto.SeatCategoryDto;
import EventTicketing.dto.VenueDto;
import EventTicketing.exception.ForbiddenActionException;
import EventTicketing.exception.InvalidStateTransitionException;
import EventTicketing.exception.ResourceNotFoundException;
import EventTicketing.model.Event;
import EventTicketing.model.SeatCategory;
import EventTicketing.model.User;
import EventTicketing.model.Venue;
import EventTicketing.model.enums.UserRole;
import EventTicketing.repository.BookingRepository;
import EventTicketing.repository.EventRepository;
import EventTicketing.repository.SeatCategoryRepository;
import EventTicketing.repository.UserRepository;
import EventTicketing.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final SeatCategoryRepository seatCategoryRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @Transactional
    public EventDto.Response create(EventDto.CreateRequest request, User organizer) {
        validateEventTiming(request.eventDate(), request.eventTime());

        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + request.venueId()));
        if (venue.getStatus() != Venue.Status.APPROVED) {
            throw new ForbiddenActionException("Events may only be created for approved venues.");
        }

        Event event = Event.builder()
                .title(request.title())
                .description(request.description())
                .category(request.category())
                .eventDate(request.eventDate())
                .eventTime(request.eventTime())
                .status(Event.Status.DRAFT)
                .venue(venue)
                .organizer(organizer)
                .build();

        Event saved = eventRepository.save(event);
        return toResponse(saved);
    }

    public PageResponse<EventDto.Summary> browsePublished(
            Event.Category category,
            UUID venueId,
            UUID organizerId,
            LocalDate dateFrom,
            LocalDate dateTo,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Event> publishedPage = eventRepository.findFilteredPublished(
                Event.Status.PUBLISHED,
                category,
                venueId,
                organizerId,
                dateFrom,
                dateTo,
                minPrice,
                maxPrice,
                pageable);

        List<EventDto.Summary> content = publishedPage.getContent().stream().map(this::toSummary).toList();
        return new PageResponse<>(content, publishedPage.getNumber(), publishedPage.getSize(),
                publishedPage.getTotalElements(), publishedPage.getTotalPages());
    }

    public PageResponse<EventDto.Summary> listOrganizerEvents(
            User organizer,
            Event.Status status,
            Event.Category category,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Event> results = eventRepository.findFilteredForAdmin(
                status, category, null, organizer.getId(), null, null, null, null, pageable);

        List<EventDto.Summary> content = results.getContent().stream().map(this::toSummary).toList();
        return new PageResponse<>(content, results.getNumber(), results.getSize(),
                results.getTotalElements(), results.getTotalPages());
    }

    public EventDto.Response getOrganizerById(UUID eventId, User organizer) {
        Event event = findOwnedEvent(eventId, organizer);
        return toResponse(event);
    }

    public EventDto.Response getPublicById(UUID eventId) {
        Event event = eventRepository.findByIdAndStatus(eventId, Event.Status.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published event not found with id: " + eventId));
        return toResponse(event);
    }

    @Transactional
    public EventDto.Response update(UUID eventId, EventDto.UpdateRequest request, User organizer) {
        Event event = findOwnedEvent(eventId, organizer);

        if (request.title() != null)
            event.setTitle(request.title());
        if (request.description() != null)
            event.setDescription(request.description());
        if (request.category() != null)
            event.setCategory(request.category());

        LocalDate updatedDate = event.getEventDate();
        LocalTime updatedTime = event.getEventTime();
        if (request.eventDate() != null) {
            updatedDate = request.eventDate();
            event.setEventDate(updatedDate);
        }
        if (request.eventTime() != null) {
            updatedTime = request.eventTime();
            event.setEventTime(updatedTime);
        }
        if (request.eventDate() != null || request.eventTime() != null) {
            validateEventTiming(updatedDate, updatedTime);
        }

        if (request.venueId() != null) {
            Venue venue = venueRepository.findById(request.venueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + request.venueId()));
            if (venue.getStatus() != Venue.Status.APPROVED) {
                throw new ForbiddenActionException("Event venue must be approved.");
            }
            event.setVenue(venue);
        }

        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventDto.Response publish(UUID eventId, User organizer) {
        Event event = findOwnedEvent(eventId, organizer);
        if (event.getStatus() != Event.Status.DRAFT) {
            throw new InvalidStateTransitionException("Only draft events can be published.");
        }
        validateEventTiming(event.getEventDate(), event.getEventTime());

        if (event.getVenue().getStatus() != Venue.Status.APPROVED) {
            throw new ForbiddenActionException("Event cannot be published with an unapproved venue.");
        }

        List<SeatCategory> categories = seatCategoryRepository.findByEvent(event);
        boolean hasValidCategory = categories.stream()
                .anyMatch(category -> category.getTotalSeats() != null && category.getTotalSeats() > 0);
        if (!hasValidCategory) {
            throw new ForbiddenActionException(
                    "Event must have at least one seat category with total seats greater than zero before publishing.");
        }

        event.setStatus(Event.Status.PUBLISHED);
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventDto.Response cancel(UUID eventId, User organizer) {
        Event event = findOwnedEvent(eventId, organizer);
        if (event.getStatus() == Event.Status.CANCELLED) {
            throw new InvalidStateTransitionException("Event is already cancelled.");
        }
        event.setStatus(Event.Status.CANCELLED);
        bookingService.cancelBookingsForEvent(eventId);
        return toResponse(eventRepository.save(event));
    }

    // ---- Admin-only operations (§6.5): no ownership restriction applies to ADMIN (§4.4, decision #2) ----

    public PageResponse<EventDto.Summary> adminList(
            Event.Status status,
            Event.Category category,
            UUID venueId,
            UUID organizerId,
            LocalDate dateFrom,
            LocalDate dateTo,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Event> results = eventRepository.findFilteredForAdmin(
                status, category, venueId, organizerId, dateFrom, dateTo, minPrice, maxPrice, pageable);

        List<EventDto.Summary> content = results.getContent().stream().map(this::toSummary).toList();
        return new PageResponse<>(content, results.getNumber(), results.getSize(),
                results.getTotalElements(), results.getTotalPages());
    }

    public EventDto.Response adminGetById(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        return toResponse(event);
    }

    @Transactional
    public EventDto.Response adminUpdate(UUID eventId, EventDto.AdminUpdateRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        if (request.title() != null)
            event.setTitle(request.title());
        if (request.description() != null)
            event.setDescription(request.description());
        if (request.category() != null)
            event.setCategory(request.category());

        LocalDate updatedDate = event.getEventDate();
        LocalTime updatedTime = event.getEventTime();
        if (request.eventDate() != null) {
            updatedDate = request.eventDate();
            event.setEventDate(updatedDate);
        }
        if (request.eventTime() != null) {
            updatedTime = request.eventTime();
            event.setEventTime(updatedTime);
        }
        if (request.eventDate() != null || request.eventTime() != null) {
            validateEventTiming(updatedDate, updatedTime);
        }

        if (request.venueId() != null) {
            Venue venue = venueRepository.findById(request.venueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + request.venueId()));
            event.setVenue(venue);
        }

        if (request.status() != null) {
            event.setStatus(request.status());
        }

        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public void adminDelete(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        if (bookingRepository.existsByEventId(eventId)) {
            throw new ForbiddenActionException(
                    "This event has existing bookings and cannot be deleted; cancel it instead.");
        }
        eventRepository.delete(event);
    }

    private Event findOwnedEvent(UUID eventId, User organizer) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        if (!event.getOrganizer().getId().equals(organizer.getId()) && organizer.getRole() != UserRole.ADMIN) {
            throw new ForbiddenActionException("You do not have permission to manage this event.");
        }
        return event;
    }

    private void validateEventTiming(LocalDate eventDate, LocalTime eventTime) {
        if (eventDate == null || eventTime == null) {
            throw new IllegalArgumentException("Event date and time must be provided.");
        }
        LocalDateTime eventDateTime = LocalDateTime.of(eventDate, eventTime);
        if (!eventDateTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Event date and time must be in the future.");
        }
    }

    private EventDto.Response toResponse(Event event) {
        List<SeatCategoryDto.Summary> seats = seatCategoryRepository.findByEvent(event).stream()
                .map(SeatCategoryDto.Summary::from).toList();
        return new EventDto.Response(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                event.getEventDate(),
                event.getEventTime(),
                event.getStatus(),
                VenueDto.Summary.from(event.getVenue()),
                event.getOrganizer().getId(),
                seats,
                event.getCreatedAt(),
                event.getUpdatedAt());
    }

    private EventDto.Summary toSummary(Event event) {
        return new EventDto.Summary(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                event.getEventDate(),
                event.getEventTime(),
                event.getStatus(),
                event.getVenue().getId(),
                event.getOrganizer().getId(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }
}