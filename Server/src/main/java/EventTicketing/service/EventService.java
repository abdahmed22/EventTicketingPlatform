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

/**
 * Person 2 Service: EventService
 * Handles business logic for Event creation, filtering/browsing, publishing,
 * status checks, and cancellation cascade.
 */
@Service
// Lombok generates a constructor with one parameter per "final" field below,
// so Spring can inject all these dependencies without writing the constructor by hand.
@RequiredArgsConstructor
public class EventService {

    // --- Dependencies injected by Spring (all final -> immutable, required at construction time) ---
    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final SeatCategoryRepository seatCategoryRepository;

    /**
     * Creates a new Event in DRAFT status.
     * Rule: Event date/time must be in the future, and Venue must be APPROVED.
     */
    @Transactional // wraps this method in a DB transaction; rolls back if an exception is thrown
    public EventDto.Response create(EventDto.CreateRequest request, User organizer) {
        // Guard clause: reject if the event date/time is missing or in the past
        validateEventTiming(request.eventDate(), request.eventTime());

        // Look up the venue; 404-style error if it doesn't exist
        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + request.venueId()));

        // Business rule: you can only host events at venues that have been approved by an admin
        if (venue.getStatus() != Venue.Status.APPROVED) {
            throw new ForbiddenActionException("Events may only be created for approved venues.");
        }

        // Build the new Event entity. Always starts as DRAFT — organizer must
        // explicitly "publish" it later (see publish() below) once it's ready.
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
        return toResponse(saved); // convert entity -> DTO before returning to caller
    }

    /**
     * Public Browse: Returns a paginated list of PUBLISHED events matching filters
     * (category, venue, organizer, date range, price range).
     */
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

        // Sanity check on the date range filter before hitting the DB
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("Start date (Date From) cannot be after end date (Date To).");
        }

        Pageable pageable = PageRequest.of(page, size);

        // Delegate to the repository's filtered query. Status is hardcoded to PUBLISHED here
        // because this method is for the public-facing browse page — drafts/ canceled events
        // should never show up to regular users.
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

        // Map entity Page -> DTO Summary list, then wrap in our own PageResponse
        List<EventDto.Summary> content = publishedPage.getContent().stream().map(this::toSummary).toList();
        return new PageResponse<>(content, publishedPage.getNumber(), publishedPage.getSize(),
                publishedPage.getTotalElements(), publishedPage.getTotalPages());
    }

    /**
     * Lets an organizer view their own events (any status), optionally filtered
     * by status/category. Reuses the admin-style query since it supports an
     * optional (nullable) status filter, but scopes results to this organizer only.
     */
    public PageResponse<EventDto.Summary> listOrganizerEvents(
            User organizer,
            Event.Status status,
            Event.Category category,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size);

        // organizerId is fixed to the current user; venue/date/price filters are unused (null) here.
        Page<Event> results = eventRepository.findFilteredForAdmin(
                status, category, null, organizer.getId(), null, null, null, null, pageable);

        List<EventDto.Summary> content = results.getContent().stream().map(this::toSummary).toList();
        return new PageResponse<>(content, results.getNumber(), results.getSize(),
                results.getTotalElements(), results.getTotalPages());
    }

    /** Organizer-facing "get one event" — enforces ownership (or admin) via findOwnedEvent(). */
    public EventDto.Response getOrganizerById(UUID eventId, User organizer) {
        Event event = findOwnedEvent(eventId, organizer);
        return toResponse(event);
    }

    /** Public-facing "get one event" — only returns it if the event is actually PUBLISHED. */
    public EventDto.Response getPublicById(UUID eventId) {
        Event event = eventRepository.findByIdAndStatus(eventId, Event.Status.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published event not found with id: " + eventId));
        return toResponse(event);
    }

    /**
     * Organizer edits their own event. Uses a "PATCH-style" partial update:
     * only fields that are non-null in the request get applied.
     */
    @Transactional
    public EventDto.Response update(UUID eventId, EventDto.UpdateRequest request, User organizer) {
        // Throws if the event doesn't exist or doesn't belong to this organizer (unless they are an ADMIN)
        Event event = findOwnedEvent(eventId, organizer);

        // Only overwrite fields that were actually provided in the request
        if (request.title() != null)
            event.setTitle(request.title());
        if (request.description() != null)
            event.setDescription(request.description());
        if (request.category() != null)
            event.setCategory(request.category());

        // Track the "effective" date/time (existing value unless overridden) so we can
        // validate the combined date+time even if only one of the two was changed.
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
        // Re-validate timing only if date and/or time actually changed
        if (request.eventDate() != null || request.eventTime() != null) {
            validateEventTiming(updatedDate, updatedTime);
        }

        // If a new venue was requested, make sure it exists and is approved before swapping it in
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

    /**
     * Publishes an Event (DRAFT -> PUBLISHED).
     * Requirements: Event must be DRAFT, venue must be APPROVED, date/time in the future,
     * and must have at least 1 SeatCategory with totalSeats > 0.
     */
    @Transactional
    public EventDto.Response publish(UUID eventId, User organizer) {
        Event event = findOwnedEvent(eventId, organizer);

        // State machine guard: can only publish from DRAFT (prevents re-publishing
        // an already-published or canceled event)
        if (event.getStatus() != Event.Status.DRAFT) {
            throw new InvalidStateTransitionException("Only draft events can be published.");
        }

        // Re-check timing in case the event date has since drifted into the past
        validateEventTiming(event.getEventDate(), event.getEventTime());

        // Re-check venue approval in case it was revoked after the event was created
        if (event.getVenue().getStatus() != Venue.Status.APPROVED) {
            throw new ForbiddenActionException("Event cannot be published with an unapproved venue.");
        }

        // An event needs at least one "real" seat category (with sellable seats)
        // before it can go live — otherwise nobody could book anything.
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

    /**
     * Cancels an Event and cascades cancellation to all associated active bookings.
     */
    @Transactional
    public EventDto.Response cancel(UUID eventId, User organizer) {
        Event event = findOwnedEvent(eventId, organizer);

        // Prevent double-cancellation
        if (event.getStatus() == Event.Status.CANCELLED) {
            throw new InvalidStateTransitionException("Event is already cancelled.");
        }

        event.setStatus(Event.Status.CANCELLED);

        // Cascade: cancel all bookings tied to this event (e.g. trigger refunds,
        // free up seats, notify attendees — handled inside BookingService)
        bookingService.cancelBookingsForEvent(eventId);

        return toResponse(eventRepository.save(event));
    }

    // ---- Admin-only operations (§6.5): no ownership restriction applies to ADMIN (§4.4, decision #2) ----

    /** Admin browse: like browsePublished(), but status is optional and no ownership check applies. */
    //get events with no ownership constraint
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
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("Start date (Date From) cannot be after end date (Date To).");
        }
        Pageable pageable = PageRequest.of(page, size);

        // Full filter set is exposed to admins, and status=null means "all statuses"
        Page<Event> results = eventRepository.findFilteredForAdmin(
                status, category, venueId, organizerId, dateFrom, dateTo, minPrice, maxPrice, pageable);

        List<EventDto.Summary> content = results.getContent().stream().map(this::toSummary).toList();
        return new PageResponse<>(content, results.getNumber(), results.getSize(),
                results.getTotalElements(), results.getTotalPages());
    }

    /** Admin "get one event" — no status filter, no ownership check (admins can view any event). */
    public EventDto.Response adminGetById(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        return toResponse(event);
    }

    /**
     * Admin edit — same partial-update pattern as update(), but with no ownership
     * check and the added ability to directly force-set the event's status
     * (e.g. an admin manually cancelling or re-publishing an event).
     */
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

        // Note: unlike the organizer update(), this does NOT re-check venue.status == APPROVED.
        // Admins are trusted to assign any venue.
        if (request.venueId() != null) {
            Venue venue = venueRepository.findById(request.venueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + request.venueId()));
            event.setVenue(venue);
        }

        // Admins can directly override status (bypassing the normal DRAFT->PUBLISHED->CANCELLED
        // state-machine rules enforced in publish()/cancel() above)
        if (request.status() != null) {
            event.setStatus(request.status());
        }

        return toResponse(eventRepository.save(event));
    }

    /**
     * Admin hard-delete. Only allowed if the event has no bookings at all —
     * otherwise data integrity would break (bookings pointing to a deleted event).
     * If bookings exist, the admin should cancel() instead of delete().
     */
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

    // ---------------- Private helper methods ----------------

    /**
     * Fetches an event by ID and enforces ownership: the caller must either be
     * the event's organizer, or have the ADMIN role (admins bypass ownership checks).
     * Centralizing this logic avoids repeating the same check in every organizer-facing method.
     */
    private Event findOwnedEvent(UUID eventId, User organizer) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        if (!event.getOrganizer().getId().equals(organizer.getId()) && organizer.getRole() != UserRole.ADMIN) {
            throw new ForbiddenActionException("You do not have permission to manage this event.");
        }
        return event;
    }

    /**
     * Shared validation used by create/update/publish/adminUpdate:
     * both date and time must be present, and their combination must be strictly
     * in the future (an event can't be created or scheduled for "now" or the past).
     */
    private void validateEventTiming(LocalDate eventDate, LocalTime eventTime) {
        if (eventDate == null || eventTime == null) {
            throw new IllegalArgumentException("Event date and time must be provided.");
        }
        LocalDateTime eventDateTime = LocalDateTime.of(eventDate, eventTime);
        if (!eventDateTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Event date and time must be in the future.");
        }
    }

    /**
     * Maps an Event entity -> full Response DTO (includes seat category details).
     * Used for single-event endpoints (create, get, update, publish, cancel, admin*).
     */
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

    /**
     * Maps an Event entity -> lightweight Summary DTO (no seat category details).
     * Used for list/browse endpoints where fetching full seat data for every
     * row would be wasteful (avoids N+1-style overhead on paginated lists).
     */
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