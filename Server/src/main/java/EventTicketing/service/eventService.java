package EventTicketing.service;

import EventTicketing.dto.PageResponse;
import EventTicketing.dto.eventDto;
import EventTicketing.exception.DuplicateResourceException;
import EventTicketing.exception.ResourceNotFoundException;
import EventTicketing.model.Event;
import EventTicketing.repository.eventRepository;
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

    @Transactional
    public eventDto.Response create(eventDto.CreateRequest request) {
        if (request.dateTime() == null || !request.dateTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Event dateTime must be in the future.");
        }

        Event event = new Event();
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setCategory(request.category());
        event.setDateTime(request.dateTime());
        event.setVenueId(request.venueId());
        event.setOrganizerId(request.organizerId());
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
        if (request.dateTime() != null)
            event.setDateTime(request.dateTime());
        if (request.venueId() != null)
            event.setVenueId(request.venueId());

        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public eventDto.Response publish(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        if (event.getDateTime() == null || !event.getDateTime().isAfter(LocalDateTime.now())) {
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

    private eventDto.Response toResponse(Event event) {
        return new eventDto.Response(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                event.getDateTime(),
                event.getStatus(),
                event.getVenueId(),
                event.getOrganizerId(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }

    private eventDto.Summary toSummary(Event event) {
        return new eventDto.Summary(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                event.getDateTime(),
                event.getStatus(),
                event.getVenueId(),
                event.getOrganizerId(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }
}
