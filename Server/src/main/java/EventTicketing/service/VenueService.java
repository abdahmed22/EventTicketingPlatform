package EventTicketing.service;

import EventTicketing.dto.VenueDto;
import EventTicketing.exception.ForbiddenActionException;
import EventTicketing.exception.ResourceNotFoundException;
import EventTicketing.model.User;
import EventTicketing.model.Venue;
import EventTicketing.repository.EventRepository;
import EventTicketing.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VenueService {

    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;

    @Transactional
    public VenueDto.Response submit(User organizer, VenueDto.CreateRequest request) {
        Venue venue = Venue.builder()
                .name(request.name())
                .address(request.address())
                .capacity(request.capacity())
                .requestedBy(organizer)
                .status(Venue.Status.PENDING)
                .build();
        return VenueDto.Response.from(venueRepository.save(venue));
    }

    public List<VenueDto.Response> listMyVenues(User organizer, Venue.Status status) {
        if (status == Venue.Status.APPROVED) {
            return venueRepository.findByStatus(Venue.Status.APPROVED).stream()
                    .map(VenueDto.Response::from)
                    .toList();
        }
        return venueRepository.findByRequestedBy(organizer).stream()
                .map(VenueDto.Response::from)
                .toList();
    }

    public List<VenueDto.Response> listPendingOrAll(Venue.Status status) {
        if (status == null) {
            return venueRepository.findAll().stream().map(VenueDto.Response::from).toList();
        }
        return venueRepository.findByStatus(status).stream()
                .map(VenueDto.Response::from)
                .toList();
    }

    @Transactional
    public VenueDto.Response adminCreate(User admin, VenueDto.CreateRequest request) {
        Venue venue = Venue.builder()
                .name(request.name())
                .address(request.address())
                .capacity(request.capacity())
                .requestedBy(admin)
                .status(Venue.Status.APPROVED)
                .reviewedAt(LocalDateTime.now())
                .reviewedBy(admin)
                .build();
        return VenueDto.Response.from(venueRepository.save(venue));
    }

    @Transactional
    public VenueDto.Response adminUpdate(UUID venueId, VenueDto.UpdateRequest request) {
        Venue venue = getVenue(venueId);
        venue.setName(request.name());
        venue.setAddress(request.address());
        venue.setCapacity(request.capacity());
        return VenueDto.Response.from(venueRepository.save(venue));
    }

    @Transactional
    public void adminDelete(UUID venueId) {
        Venue venue = getVenue(venueId);
        if (eventRepository.existsByVenueId(venueId)) {
            throw new ForbiddenActionException("Venue cannot be deleted because it is linked to one or more events.");
        }
        venueRepository.delete(venue);
    }

    @Transactional
    public VenueDto.Response approve(UUID venueId, User admin) {
        Venue venue = getVenue(venueId);
        venue.setStatus(Venue.Status.APPROVED);
        venue.setReviewedAt(LocalDateTime.now());
        venue.setReviewedBy(admin);
        return VenueDto.Response.from(venueRepository.save(venue));
    }

    @Transactional
    public VenueDto.Response reject(UUID venueId, User admin) {
        Venue venue = getVenue(venueId);
        venue.setStatus(Venue.Status.REJECTED);
        venue.setReviewedAt(LocalDateTime.now());
        venue.setReviewedBy(admin);
        return VenueDto.Response.from(venueRepository.save(venue));
    }

    private Venue getVenue(UUID venueId) {
        return venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + venueId));
    }
}
