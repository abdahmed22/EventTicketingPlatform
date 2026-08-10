package EventTicketing.service;

import EventTicketing.dto.EventDto;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private SeatCategoryRepository seatCategoryRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingService bookingService;

    @InjectMocks
    private EventService eventService;

    private User organizer;
    private Venue approvedVenue;
    private Venue pendingVenue;
    private Event draftEvent;
    private UUID eventId;
    private UUID venueId;

    @BeforeEach
    void setUp() {
        organizer = User.builder()
                .id(UUID.randomUUID())
                .name("Organizer")
                .email("organizer@example.com")
                .role(UserRole.ORGANIZER)
                .build();

        venueId = UUID.randomUUID();
        approvedVenue = Venue.builder()
                .id(venueId)
                .name("Cairo Hall")
                .address("Cairo")
                .capacity(500)
                .status(Venue.Status.APPROVED)
                .build();

        pendingVenue = Venue.builder()
                .id(UUID.randomUUID())
                .name("Pending Hall")
                .address("Giza")
                .capacity(200)
                .status(Venue.Status.PENDING)
                .build();

        eventId = UUID.randomUUID();
        draftEvent = Event.builder()
                .id(eventId)
                .title("Tech Conference")
                .description("Annual Tech Conf")
                .category(Event.Category.CONFERENCE)
                .eventDate(LocalDate.now().plusDays(10))
                .eventTime(LocalTime.of(14, 0))
                .status(Event.Status.DRAFT)
                .venue(approvedVenue)
                .organizer(organizer)
                .build();
    }

    @Test
    void create_shouldCreateDraftEvent_whenVenueIsApproved() {
        EventDto.CreateRequest request = new EventDto.CreateRequest(
                "Tech Conference", "Annual Tech Conf", Event.Category.CONFERENCE,
                LocalDate.now().plusDays(10), LocalTime.of(14, 0), venueId);

        when(venueRepository.findById(venueId)).thenReturn(Optional.of(approvedVenue));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventDto.Response response = eventService.create(request, organizer);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Tech Conference");
        assertThat(response.status()).isEqualTo(Event.Status.DRAFT);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void create_shouldThrowForbidden_whenVenueNotApproved() {
        EventDto.CreateRequest request = new EventDto.CreateRequest(
                "Tech Conference", "Annual Tech Conf", Event.Category.CONFERENCE,
                LocalDate.now().plusDays(10), LocalTime.of(14, 0), pendingVenue.getId());

        when(venueRepository.findById(pendingVenue.getId())).thenReturn(Optional.of(pendingVenue));

        assertThatThrownBy(() -> eventService.create(request, organizer))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessageContaining("approved venues");
    }

    @Test
    void publish_shouldPublishEvent_whenValid() {
        SeatCategory seatCategory = SeatCategory.builder()
                .id(UUID.randomUUID())
                .event(draftEvent)
                .venue(approvedVenue)
                .name("Standard")
                .totalSeats(100)
                .availableSeats(100)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));
        when(seatCategoryRepository.findByEvent(draftEvent)).thenReturn(List.of(seatCategory));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventDto.Response response = eventService.publish(eventId, organizer);

        assertThat(response.status()).isEqualTo(Event.Status.PUBLISHED);
        verify(eventRepository).save(draftEvent);
    }

    @Test
    void publish_shouldThrowException_whenNoSeatCategoriesExist() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));
        when(seatCategoryRepository.findByEvent(draftEvent)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> eventService.publish(eventId, organizer))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessageContaining("at least one seat category");
    }

    @Test
    void cancel_shouldCancelEventAndCascadeBookings() {
        draftEvent.setStatus(Event.Status.PUBLISHED);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventDto.Response response = eventService.cancel(eventId, organizer);

        assertThat(response.status()).isEqualTo(Event.Status.CANCELLED);
        verify(bookingService).cancelBookingsForEvent(eventId);
    }

    @Test
    void cancel_shouldThrowException_whenAlreadyCancelled() {
        draftEvent.setStatus(Event.Status.CANCELLED);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));

        assertThatThrownBy(() -> eventService.cancel(eventId, organizer))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
