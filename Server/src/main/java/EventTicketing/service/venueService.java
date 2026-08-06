package EventTicketing.service;

import EventTicketing.dto.venueDto;
import EventTicketing.exception.ForbiddenActionException;
import EventTicketing.exception.ResourceNotFoundException;
import EventTicketing.model.User;
import EventTicketing.model.Venue;
import EventTicketing.model.enums.UserRole;
import EventTicketing.repository.venueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class venueService {

    private final venueRepository venueRepository;

    @Transactional
    public venueDto.Response submit(User organizer, venueDto.CreateRequest request) {
        Venue venue = Venue.builder()
                .name(request.name())
                .address(request.address())
                .capacity(request.capacity())
                .requestedBy(organizer)
                .status(Venue.Status.PENDING)
                .build();
        return venueDto.Response.from(venueRepository.save(venue));
    }

    public List<venueDto.Response> listMyVenues(User organizer) {
        return venueRepository.findByRequestedBy(organizer).stream()
                .map(venueDto.Response::from)
                .toList();
    }

    public List<venueDto.Response> listPendingOrAll(Venue.Status status) {
        if (status == null) {
            return venueRepository.findAll().stream().map(venueDto.Response::from).toList();
        }
        return venueRepository.findAll().stream()
                .filter(venue -> venue.getStatus() == status)
                .map(venueDto.Response::from)
                .toList();
    }

    @Transactional
    public venueDto.Response approve(UUID venueId, User admin) {
        ensureAdmin(admin);
        Venue venue = getVenue(venueId);
        venue.setStatus(Venue.Status.APPROVED);
        venue.setReviewedAt(LocalDateTime.now());
        venue.setReviewedBy(admin);
        return venueDto.Response.from(venueRepository.save(venue));
    }

    @Transactional
    public venueDto.Response reject(UUID venueId, User admin, venueDto.RejectRequest request) {
        ensureAdmin(admin);
        Venue venue = getVenue(venueId);
        venue.setStatus(Venue.Status.REJECTED);
        venue.setReviewedAt(LocalDateTime.now());
        venue.setReviewedBy(admin);
        return venueDto.Response.from(venueRepository.save(venue));
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
}
