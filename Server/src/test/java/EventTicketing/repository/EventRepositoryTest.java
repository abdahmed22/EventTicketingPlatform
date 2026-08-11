package EventTicketing.repository;

import EventTicketing.model.Event;
import EventTicketing.model.User;
import EventTicketing.model.Venue;
import EventTicketing.model.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Test
    void testFindFilteredPublished_withDateRange() {
        User organizer = userRepository.save(User.builder()
                .name("Org")
                .email("org@test.com")
                .password("pass")
                .role(UserRole.ORGANIZER)
                .build());

        Venue venue = venueRepository.save(Venue.builder()
                .name("Venue")
                .address("Address")
                .capacity(100)
                .requestedBy(organizer)
                .status(Venue.Status.APPROVED)
                .build());

        Event event = eventRepository.save(Event.builder()
                .title("Test Event")
                .description("Desc")
                .category(Event.Category.MUSIC)
                .eventDate(LocalDate.of(2026, 8, 15))
                .eventTime(LocalTime.of(20, 0))
                .status(Event.Status.PUBLISHED)
                .venue(venue)
                .organizer(organizer)
                .build());

        Page<Event> page = eventRepository.findFilteredPublished(
                Event.Status.PUBLISHED,
                null,
                null,
                null,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(page).isNotNull();
        assertThat(page.getContent()).extracting(Event::getTitle).contains("Test Event");
    }
}
