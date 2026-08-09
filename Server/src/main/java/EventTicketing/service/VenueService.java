package EventTicketing.service;

import EventTicketing.dto.DashboardDTO;
import EventTicketing.dto.VenueDto;
import EventTicketing.exception.ForbiddenActionException;
import EventTicketing.exception.ResourceNotFoundException;
import EventTicketing.model.User;
import EventTicketing.model.Venue;
import EventTicketing.model.enums.UserRole;
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

    public List<VenueDto.Response> listMyVenues(User organizer) {
        return venueRepository.findByRequestedBy(organizer).stream()
                .map(VenueDto.Response::from)
                .toList();
    }

    public List<VenueDto.Response> listPendingOrAll(Venue.Status status) {
        if (status == null) {
            return venueRepository.findAll().stream().map(VenueDto.Response::from).toList();
        }
        return venueRepository.findAll().stream()
                .filter(venue -> venue.getStatus() == status)
                .map(VenueDto.Response::from)
                .toList();
    }

    @Transactional
    public VenueDto.Response approve(UUID venueId, User admin) {
        ensureAdmin(admin);
        Venue venue = getVenue(venueId);
        venue.setStatus(Venue.Status.APPROVED);
        venue.setReviewedAt(LocalDateTime.now());
        venue.setReviewedBy(admin);
        return VenueDto.Response.from(venueRepository.save(venue));
    }

    @Transactional
    public VenueDto.Response reject(UUID venueId, User admin, VenueDto.RejectRequest request) {
        ensureAdmin(admin);
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

    private void ensureAdmin(User user) {
        if (user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenActionException("Only admin users may perform this action.");
        }
    }

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

    // انا عامل ديه خاصه بالdashboard بس 
    @Transactional
    public VenueDto.Response rejectFromDashboard(UUID venueId, User admin) {
        Venue venue = getVenue(venueId);
        venue.setStatus(Venue.Status.REJECTED);
        venue.setReviewedAt(LocalDateTime.now());
        venue.setReviewedBy(admin);
        return VenueDto.Response.from(venueRepository.save(venue));
    }
}
