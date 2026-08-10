package EventTicketing.service;

import EventTicketing.dto.SeatCategoryDto;
import EventTicketing.exception.ForbiddenActionException;
import EventTicketing.model.Event;
import EventTicketing.model.SeatCategory;
import EventTicketing.model.User;
import EventTicketing.model.Venue;
import EventTicketing.model.enums.UserRole;
import EventTicketing.repository.EventRepository;
import EventTicketing.repository.SeatCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatCategoryServiceTest {

    @Mock
    private SeatCategoryRepository seatCategoryRepository;
    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private SeatCategoryService seatCategoryService;

    private User organizer;
    private Event event;
    private Venue venue;
    private SeatCategory seatCategory;
    private UUID eventId;
    private UUID seatCategoryId;

    @BeforeEach
    void setUp() {
        organizer = User.builder()
                .id(UUID.randomUUID())
                .name("Organizer")
                .role(UserRole.ORGANIZER)
                .build();

        venue = Venue.builder()
                .id(UUID.randomUUID())
                .name("Hall 1")
                .status(Venue.Status.APPROVED)
                .build();

        eventId = UUID.randomUUID();
        event = Event.builder()
                .id(eventId)
                .title("Concert")
                .organizer(organizer)
                .venue(venue)
                .status(Event.Status.DRAFT)
                .build();

        seatCategoryId = UUID.randomUUID();
        seatCategory = SeatCategory.builder()
                .id(seatCategoryId)
                .event(event)
                .venue(venue)
                .name("VIP")
                .price(BigDecimal.valueOf(150))
                .totalSeats(50)
                .availableSeats(50)
                .seatingCapacity(1)
                .build();
    }

    @Test
    void create_shouldCreateSeatCategory() {
        SeatCategoryDto.CreateRequest request = new SeatCategoryDto.CreateRequest("VIP", BigDecimal.valueOf(150), 50, 1);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(seatCategoryRepository.save(any(SeatCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        SeatCategoryDto.Response response = seatCategoryService.create(eventId, organizer, request);

        assertThat(response.name()).isEqualTo("VIP");
        assertThat(response.totalSeats()).isEqualTo(50);
        assertThat(response.availableSeats()).isEqualTo(50);
        verify(seatCategoryRepository).save(any(SeatCategory.class));
    }

    @Test
    void update_shouldAdjustAvailableSeatsDelta() {
        SeatCategoryDto.UpdateRequest request = new SeatCategoryDto.UpdateRequest(null, null, 70, null);
        when(seatCategoryRepository.findById(seatCategoryId)).thenReturn(Optional.of(seatCategory));
        when(seatCategoryRepository.save(any(SeatCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        SeatCategoryDto.Response response = seatCategoryService.update(seatCategoryId, organizer, request);

        assertThat(response.totalSeats()).isEqualTo(70);
        assertThat(response.availableSeats()).isEqualTo(70); // 50 original + 20 delta
    }

    @Test
    void update_shouldRejectReductionBelowReservedSeats() {
        seatCategory.setTotalSeats(100);
        seatCategory.setAvailableSeats(30); // 70 reserved

        SeatCategoryDto.UpdateRequest request = new SeatCategoryDto.UpdateRequest(null, null, 50, null);
        when(seatCategoryRepository.findById(seatCategoryId)).thenReturn(Optional.of(seatCategory));

        assertThatThrownBy(() -> seatCategoryService.update(seatCategoryId, organizer, request))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessageContaining("below already reserved seats");
    }
}
