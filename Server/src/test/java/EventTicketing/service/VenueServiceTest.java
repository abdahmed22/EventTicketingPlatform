package EventTicketing.service;

import EventTicketing.dto.VenueDto;
import EventTicketing.exception.ForbiddenActionException;
import EventTicketing.exception.ResourceNotFoundException;
import EventTicketing.model.User;
import EventTicketing.model.Venue;
import EventTicketing.model.enums.UserRole;
import EventTicketing.repository.EventRepository;
import EventTicketing.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

    @Mock
    private VenueRepository venueRepository;
    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private VenueService venueService;

    private User organizer;
    private User admin;
    private Venue venue;
    private UUID venueId;

    @BeforeEach
    void setUp() {
        organizer = User.builder()
                .id(UUID.randomUUID())
                .name("Organizer")
                .role(UserRole.ORGANIZER)
                .build();

        admin = User.builder()
                .id(UUID.randomUUID())
                .name("Admin")
                .role(UserRole.ADMIN)
                .build();

        venueId = UUID.randomUUID();
        venue = Venue.builder()
                .id(venueId)
                .name("Stadium A")
                .address("Location A")
                .capacity(1000)
                .requestedBy(organizer)
                .status(Venue.Status.PENDING)
                .build();
    }

    @Test
    void submit_shouldCreatePendingVenue() {
        VenueDto.CreateRequest request = new VenueDto.CreateRequest("Stadium A", "Location A", 1000);
        when(venueRepository.save(any(Venue.class))).thenAnswer(inv -> inv.getArgument(0));

        VenueDto.Response response = venueService.submit(organizer, request);

        assertThat(response.status()).isEqualTo(Venue.Status.PENDING);
        assertThat(response.name()).isEqualTo("Stadium A");
        verify(venueRepository).save(any(Venue.class));
    }

    @Test
    void listMyVenues_shouldReturnApprovedVenues_whenStatusApproved() {
        Venue approvedVenue = Venue.builder()
                .id(UUID.randomUUID())
                .name("Shared Hall")
                .requestedBy(organizer)
                .status(Venue.Status.APPROVED)
                .build();

        when(venueRepository.findByRequestedByAndStatus(organizer, Venue.Status.APPROVED)).thenReturn(List.of(approvedVenue));

        List<VenueDto.Response> responses = venueService.listMyVenues(organizer, Venue.Status.APPROVED);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("Shared Hall");
        verify(venueRepository).findByRequestedByAndStatus(organizer, Venue.Status.APPROVED);
    }

    @Test
    void approve_shouldSetStatusToApproved() {
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(venueRepository.save(any(Venue.class))).thenAnswer(inv -> inv.getArgument(0));

        VenueDto.Response response = venueService.approve(venueId, admin);

        assertThat(response.status()).isEqualTo(Venue.Status.APPROVED);
        verify(venueRepository).save(venue);
    }

    @Test
    void adminDelete_shouldThrowException_whenLinkedToEvent() {
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(eventRepository.existsByVenueId(venueId)).thenReturn(true);

        assertThatThrownBy(() -> venueService.adminDelete(venueId))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessageContaining("linked to one or more events");
    }
}
