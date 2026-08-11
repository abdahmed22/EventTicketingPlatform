package EventTicketing.service;

import EventTicketing.dto.DashboardDTO;
import EventTicketing.dto.VenueDto;
import EventTicketing.exception.ForbiddenActionException;
import EventTicketing.exception.ResourceNotFoundException;
import EventTicketing.model.User;
import EventTicketing.model.Venue;
import EventTicketing.model.enums.UserRole;
import EventTicketing.repository.EventRepository;
import EventTicketing.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Person 2 Service: VenueService
 * Manages venue requests (submit PENDING venue), listing venues,
 * and Admin approve/reject/create operations.
 */
@Service
@RequiredArgsConstructor
// Class-level default: every method runs in a read-only transaction unless overridden.
// Read-only transactions let Hibernate skip dirty-checking, which is a small perf win
// for the many list/get methods below. Any method that actually writes to the DB
// must override this with its own @Transactional (see submit, update, approve, etc.).
@Transactional(readOnly = true)
public class VenueService {

    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;

    /**
     * Organizer submits a new venue request. Created in PENDING status.
     * Venue isn't usable for events until an admin approves it (see approve()).
     */
    @Transactional // overrides the class-level readOnly=true, since this writes to the DB
    public VenueDto.Response submit(User organizer, VenueDto.CreateRequest request) {
        Venue venue = Venue.builder()
                .name(request.name())
                .address(request.address())
                .capacity(request.capacity())
                .requestedBy(organizer) // track who asked for this venue
                .status(Venue.Status.PENDING) // always starts pending admin review
                .build();
        return VenueDto.Response.from(venueRepository.save(venue));
    }

    /**
     * Lists venues requested by a given organizer, optionally filtered by status
     * (e.g. only PENDING, or only APPROVED). Pass status=null to get all of them.
     */
    public List<VenueDto.Response> listMyVenues(User organizer, Venue.Status status) {
        List<Venue> venues = status != null
                ? venueRepository.findByRequestedByAndStatus(organizer, status)
                : venueRepository.findByRequestedBy(organizer);
        return venues.stream()
                .map(VenueDto.Response::from)
                .toList();
    }

    /**
     * Partial update of a venue. Allowed for the venue's original requester,
     * or any ADMIN. Fields are only updated if provided (name/address must be
     * non-blank; capacity must be > 0 — this doubles as basic validation, but
     * also means "clearing" a field to blank/0 isn't supported here).
     */
    @Transactional
    public VenueDto.Response update(UUID venueId, VenueDto.CreateRequest request, User user) {
        Venue venue = getVenue(venueId);

        // Ownership check: must be the requester, OR an admin (admins bypass ownership)
        if (user.getRole() != UserRole.ADMIN && (venue.getRequestedBy() == null || !venue.getRequestedBy().getId().equals(user.getId()))) {
            throw new ForbiddenActionException("You do not have permission to edit this venue.");
        }

        if (request.name() != null && !request.name().isBlank()) {
            venue.setName(request.name());
        }
        if (request.address() != null && !request.address().isBlank()) {
            venue.setAddress(request.address());
        }
        if (request.capacity() > 0) {
            venue.setCapacity(request.capacity());
        }
        return VenueDto.Response.from(venueRepository.save(venue));
    }

    /**
     * Admin-facing venue list. Pass status=null to get every venue regardless
     * of status; otherwise filters to just that status (e.g. PENDING for a review queue).
     *
     * Note: this loads ALL venues via findAll() and filters in memory rather than
     * querying by status directly — fine for small venue counts, but won't scale
     * as well as a proper findByStatus() query if the table grows large.
     */
    public List<VenueDto.Response> listPendingOrAll(Venue.Status status) {
        if (status == null) {
            return venueRepository.findAll().stream().map(VenueDto.Response::from).toList();
        }
        return venueRepository.findAll().stream()
                .filter(venue -> venue.getStatus() == status)
                .map(VenueDto.Response::from)
                .toList();
    }

    /**
     * Admin approves a PENDING venue, making it usable for event creation by any organizer.
     */
    @Transactional
    public VenueDto.Response approve(UUID venueId, User admin) {
        ensureAdmin(admin); // hard role check — only admins can approve
        Venue venue = getVenue(venueId);
        venue.setStatus(Venue.Status.APPROVED);
        venue.setReviewedAt(LocalDateTime.now()); // audit trail: when it was reviewed
        venue.setReviewedBy(admin);                // audit trail: who reviewed it
        return VenueDto.Response.from(venueRepository.save(venue));
    }

    /**
     * Admin rejects a PENDING venue.
     * Note: the `request` (VenueDto.RejectRequest) parameter is accepted but never
     * used here — presumably intended to store a rejection reason, but that field
     * isn't currently being persisted onto the Venue. Worth double-checking if a
     * "reason" is expected to show up anywhere downstream.
     */
    @Transactional
    public VenueDto.Response reject(UUID venueId, User admin, VenueDto.RejectRequest request) {
        ensureAdmin(admin);
        Venue venue = getVenue(venueId);
        venue.setStatus(Venue.Status.REJECTED);
        venue.setReviewedAt(LocalDateTime.now());
        venue.setReviewedBy(admin);
        return VenueDto.Response.from(venueRepository.save(venue));
    }

    /**
     * Admin hard-deletes a venue — blocked if any event (past or present) is
     * still linked to it, to avoid leaving events with a dangling venue reference.
     */
    @Transactional
    public void adminDelete(UUID venueId) {
        Venue venue = getVenue(venueId);
        if (eventRepository.existsByVenueId(venueId)) {
            throw new ForbiddenActionException("Cannot delete venue: it is linked to one or more events.");
        }
        venueRepository.delete(venue);
    }

    // ---------------- Private helpers ----------------

    /** Fetches a venue by ID or throws a 404-style not-found exception. */
    private Venue getVenue(UUID venueId) {
        return venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + venueId));
    }

    /** Role guard: throws if the given user isn't an ADMIN. */
    private void ensureAdmin(User user) {
        if (user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenActionException("Only admin users may perform this action.");
        }
    }

    /**
     * Builds the "pending venue applications" widget data for the admin dashboard.
     * Maps each PENDING venue to a lightweight DTO, safely handling venues whose
     * requestedBy user may be null (defensive null-checks below rather than assuming
     * every venue has a requester).
     */
    public List<DashboardDTO.PendingVenue> getPendingApplicationsForDashboard() {
        return venueRepository.findByStatus((Venue.Status.PENDING)).stream()
                .map(venue -> new DashboardDTO.PendingVenue(
                        venue.getId(),
                        venue.getName(),
                        venue.getAddress(),
                        venue.getCapacity(),
                        venue.getRequestedBy() == null ? null : venue.getRequestedBy().getId(),
                        venue.getRequestedBy() == null ? null : venue.getRequestedBy().getName()))
                .toList();
    }

    // Special dashboard rejection endpoint for Admin dashboard review panel
    /**
     * Rejects a venue directly from the dashboard's quick-action panel.
     */
    @Transactional
    public VenueDto.Response rejectFromDashboard(UUID venueId, User admin) {
        Venue venue = getVenue(venueId);
        venue.setStatus(Venue.Status.REJECTED);
        venue.setReviewedAt(LocalDateTime.now());
        venue.setReviewedBy(admin);
        return VenueDto.Response.from(venueRepository.save(venue));
    }

    /**
     * Admin directly creates a venue. Unlike submit() (organizer path, starts PENDING),
     * this venue is auto-APPROVED immediately since an admin is vouching for it —
     * no separate review step needed. The admin is recorded as both the requester
     * and the reviewer.
     */
    @Transactional
    public VenueDto.Response adminCreate(User admin, VenueDto.CreateRequest request) {
        ensureAdmin(admin);
        Venue venue = Venue.builder()
                .name(request.name())
                .address(request.address())
                .capacity(request.capacity())
                .requestedBy(admin)
                .status(Venue.Status.APPROVED)
                .reviewedBy(admin)
                .reviewedAt(LocalDateTime.now())
                .build();
        return VenueDto.Response.from(venueRepository.save(venue));
    }
}