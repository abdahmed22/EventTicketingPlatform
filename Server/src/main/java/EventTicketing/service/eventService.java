package EventTicketing.service;

import EventTicketing.dto.PageResponse;
import EventTicketing.dto.eventDto;
import EventTicketing.exception.ResourceNotFoundException;
import EventTicketing.model.Event;
import EventTicketing.model.User;
import EventTicketing.model.Venue;
import EventTicketing.model.enums.UserRole;
import EventTicketing.repository.eventRepository;
import EventTicketing.repository.userRepository;
import EventTicketing.repository.venueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class eventService {

    private final eventRepository eventRepository;
    private final venueRepository venueRepository;
    private final userRepository userRepository;

    @Transactional
    public eventDto.Response create(eventDto.CreateRequest request) {
        if (request.dateTime() == null || !request.dateTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Event dateTime must be in the future.");
        }

        Venue venue = resolveApprovedVenue(request.venueId());
        User organizer = resolveOrganizer(request.organizerId());

        Event event = new Event();
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setCategory(request.category());
        event.setEventDate(request.dateTime().toLocalDate());
        event.setEventTime(request.dateTime().toLocalTime());
        event.setVenue(venue);
        event.setOrganizer(organizer);
        event.setStatus(Event.Status.DRAFT);

        Event saved = eventRepository.save(event);
        return toResponse(saved);
    }

    public PageResponse<eventDto.Summary> browsePublished(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Event> publishedPage = eventRepository.findByStatus(Event.Status.PUBLISHED, pageable);
        List<eventDto.Summary> content = publishedPage.getContent().stream().map(this::toSummary).toList();
        return new PageResponse<>(content, publishedPage.getNumber(), publishedPage.getSize(),
                publishedPage.getTotalElements(), publishedPage.getTotalPages());
    }

    public eventDto.Response getById(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        return toResponse(event);
    }

    @Transactional
    public eventDto.Response update(UUID eventId, eventDto.UpdateRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        if (request.title() != null)
            event.setTitle(request.title());
        if (request.description() != null)
            event.setDescription(request.description());
        if (request.category() != null)
            event.setCategory(request.category());
        if (request.dateTime() != null) {
            event.setEventDate(request.dateTime().toLocalDate());
            event.setEventTime(request.dateTime().toLocalTime());
        }
        if (request.venueId() != null)
            event.setVenue(resolveApprovedVenue(request.venueId()));

        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public eventDto.Response publish(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        LocalDateTime dateTime = combinedDateTime(event);
        if (dateTime == null || !dateTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Event dateTime must be in the future when publishing.");
        }

        event.setStatus(Event.Status.PUBLISHED);
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public eventDto.Response cancel(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        event.setStatus(Event.Status.CANCELLED);
        return toResponse(eventRepository.save(event));
    }

    private Venue resolveApprovedVenue(UUID venueId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + venueId));
        if (venue.getStatus() != Venue.Status.APPROVED) {
            throw new IllegalArgumentException("Venue must be APPROVED before it can be used for an event.");
        }
        return venue;
    }

    private User resolveOrganizer(UUID organizerId) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found with id: " + organizerId));
        if (organizer.getRole() != UserRole.ORGANIZER && organizer.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Organizer must have role ORGANIZER or ADMIN.");
        }
        return organizer;
    }

    private LocalDateTime combinedDateTime(Event event) {
        if (event.getEventDate() == null || event.getEventTime() == null) {
            return null;
        }
        return LocalDateTime.of(event.getEventDate(), event.getEventTime());
    }

    private eventDto.Response toResponse(Event event) {
        return new eventDto.Response(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                combinedDateTime(event),
                event.getStatus(),
                event.getVenue() != null ? event.getVenue().getId() : null,
                event.getOrganizer() != null ? event.getOrganizer().getId() : null,
                event.getCreatedAt(),
                event.getUpdatedAt());
    }

    private eventDto.Summary toSummary(Event event) {
        return new eventDto.Summary(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                combinedDateTime(event),
                event.getStatus(),
                event.getVenue() != null ? event.getVenue().getId() : null,
                event.getOrganizer() != null ? event.getOrganizer().getId() : null,
                event.getCreatedAt(),
                event.getUpdatedAt());
    }
}