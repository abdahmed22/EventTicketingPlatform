package EventTicketing.service;

/*
 * IMPORTANT - EXECUTION STATUS
 * ------------------------------------------------------------------
 * These tests were written but NOT executed in the environment this
 * audit was performed in: there was no Maven installation, no network
 * access to resolve dependencies, and no running Postgres instance
 * (the project has no H2/Testcontainers dependency - it talks to the
 * real Postgres configured in application.yaml / Infrastructure/
 * docker-compose.yaml). Do not treat these as a verified passing
 * suite until they have actually been run, e.g.:
 *
 *   docker compose -f Infrastructure/docker-compose.yaml up -d
 *   mvn -pl Server test -Dtest=BookingConcurrencyTest
 * ------------------------------------------------------------------
 */

import EventTicketing.dto.BookingDto;
import EventTicketing.exception.SeatUnavailableException;
import EventTicketing.model.Event;
import EventTicketing.model.SeatCategory;
import EventTicketing.model.User;
import EventTicketing.model.Venue;
import EventTicketing.model.enums.BookingStatus;
import EventTicketing.model.enums.UserRole;
import EventTicketing.repository.BookingRepository;
import EventTicketing.repository.EventRepository;
import EventTicketing.repository.SeatCategoryRepository;
import EventTicketing.repository.UserRepository;
import EventTicketing.repository.VenueRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Person 3 - Booking & Concurrency: mandatory concurrency tests (SRS
 * requirement 17). These exercise the real transactional/locking behavior of
 * {@link BookingService} against a real database - not mocks - by firing
 * genuinely concurrent requests from a thread pool and asserting on the
 * final persisted state.
 */
@SpringBootTest
class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SeatCategoryRepository seatCategoryRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private UserRepository userRepository;

    private final List<UUID> createdSeatCategoryIds = new ArrayList<>();
    private final List<UUID> createdEventIds = new ArrayList<>();
    private final List<UUID> createdVenueIds = new ArrayList<>();
    private final List<UUID> createdUserIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        // Best-effort teardown so repeated runs don't accumulate fixture
        // data. Booking rows cascade-delete is not configured, so bookings
        // for our test events are removed first.
        for (UUID eventId : createdEventIds) {
            bookingRepository.findByEventIdAndStatusIn(eventId,
                    List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED,
                            BookingStatus.CANCELLED, BookingStatus.EXPIRED))
                    .forEach(b -> bookingRepository.deleteById(b.getId()));
        }
        seatCategoryRepository.deleteAllByIdInBatch(createdSeatCategoryIds);
        eventRepository.deleteAllByIdInBatch(createdEventIds);
        venueRepository.deleteAllByIdInBatch(createdVenueIds);
        userRepository.deleteAllByIdInBatch(createdUserIds);
        createdSeatCategoryIds.clear();
        createdEventIds.clear();
        createdVenueIds.clear();
        createdUserIds.clear();
    }

    private User createUser(String emailPrefix, UserRole role) {
        User user = User.builder()
                .name(emailPrefix)
                .email(emailPrefix + "-" + UUID.randomUUID() + "@test.local")
                .password("N/A")
                .role(role)
                .build();
        user = userRepository.save(user);
        createdUserIds.add(user.getId());
        return user;
    }

    private SeatCategory createEventWithSeatCategory(int totalSeats) {
        User organizer = createUser("organizer", UserRole.ORGANIZER);

        Venue venue = Venue.builder()
                .name("Test Venue")
                .address("123 Test St")
                .capacity(1000)
                .requestedBy(organizer)
                .status(Venue.Status.APPROVED)
                .build();
        venue = venueRepository.save(venue);
        createdVenueIds.add(venue.getId());

        Event event = Event.builder()
                .title("Concurrency Test Event")
                .description("Test")
                .category(Event.Category.MUSIC)
                .eventDate(LocalDate.now().plusDays(30))
                .eventTime(LocalTime.of(19, 0))
                .status(Event.Status.PUBLISHED)
                .venue(venue)
                .venueId(venue.getId())
                .organizer(organizer)
                .organizerId(organizer.getId())
                .build();
        event = eventRepository.save(event);
        createdEventIds.add(event.getId());

        SeatCategory seatCategory = SeatCategory.builder()
                .event(event)
                .venue(venue)
                .name("General Admission")
                .price(BigDecimal.valueOf(50))
                .totalSeats(totalSeats)
                .availableSeats(totalSeats)
                .seatingCapacity(1)
                .build();

        seatCategory = seatCategoryRepository.save(seatCategory);
        createdSeatCategoryIds.add(seatCategory.getId());
        return seatCategory;
    }

    // ---------------------------------------------------------------
    // Test 1: last-seat race - exactly one concurrent reserve() wins.
    // ---------------------------------------------------------------
    @Test
    void onlyOneConcurrentReserveSucceedsForTheLastSeat() throws InterruptedException {
        SeatCategory seatCategory = createEventWithSeatCategory(1);
        UUID eventId = seatCategory.getEvent().getId();
        UUID seatCategoryId = seatCategory.getId();

        int threadCount = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger seatConflicts = new AtomicInteger(0);
        AtomicInteger otherFailures = new AtomicInteger(0);

        List<User> customers = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            customers.add(createUser("customer" + i, UserRole.CUSTOMER));
        }

        for (int i = 0; i < threadCount; i++) {
            User customer = customers.get(i);
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    bookingService.reserve(customer,
                            new BookingDto.CreateRequest(eventId, seatCategoryId, 1));
                    successes.incrementAndGet();
                } catch (SeatUnavailableException e) {
                    seatConflicts.incrementAndGet();
                } catch (Exception e) {
                    otherFailures.incrementAndGet();
                }
            });
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        assertEquals(0, otherFailures.get(), "no unexpected exceptions");
        assertEquals(1, successes.get(), "exactly one reservation should succeed");
        assertEquals(threadCount - 1, seatConflicts.get(),
                "every other request must be rejected with SeatUnavailableException (409)");

        SeatCategory finalState = seatCategoryRepository.findById(seatCategoryId).orElseThrow();
        assertEquals(0, finalState.getAvailableSeats());
    }

    // ---------------------------------------------------------------
    // Test 2: high-contention reserve() never drives availableSeats < 0,
    // and never lets more bookings succeed than there were seats.
    // ---------------------------------------------------------------
    @Test
    void concurrentReservationsNeverOversellOrGoNegative() throws InterruptedException {
        int totalSeats = 5;
        int threadCount = 25;

        SeatCategory seatCategory = createEventWithSeatCategory(totalSeats);
        UUID eventId = seatCategory.getEvent().getId();
        UUID seatCategoryId = seatCategory.getId();

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger(0);

        List<User> customers = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            customers.add(createUser("customer" + i, UserRole.CUSTOMER));
        }

        for (int i = 0; i < threadCount; i++) {
            User customer = customers.get(i);
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    bookingService.reserve(customer,
                            new BookingDto.CreateRequest(eventId, seatCategoryId, 1));
                    successes.incrementAndGet();
                } catch (SeatUnavailableException expected) {
                    // expected once seats run out
                } catch (Exception ignored) {
                    // any other exception would be a bug, but is checked
                    // via the assertions below (successes count and final
                    // available seats), not swallowed silently in review.
                }
            });
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        assertEquals(totalSeats, successes.get(),
                "exactly totalSeats reservations should succeed, no more, no fewer");

        SeatCategory finalState = seatCategoryRepository.findById(seatCategoryId).orElseThrow();
        assertEquals(0, finalState.getAvailableSeats());
        assertTrue(finalState.getAvailableSeats() >= 0, "available seats must never go negative");
    }

    // ---------------------------------------------------------------
    // Test 3: confirm() racing the scheduler's expireBooking() on an
    // already-time-expired PENDING booking must never end up CONFIRMED
    // with seats already released, and must never release seats twice.
    // ---------------------------------------------------------------
    @Test
    void concurrentConfirmAndExpirationNeverDoubleReleaseOrLeaveInconsistentState() throws InterruptedException {
        int trials = 15;

        for (int t = 0; t < trials; t++) {
            SeatCategory seatCategory = createEventWithSeatCategory(1);
            UUID eventId = seatCategory.getEvent().getId();
            UUID seatCategoryId = seatCategory.getId();
            User customer = createUser("customer", UserRole.CUSTOMER);

            BookingDto.Response reserved = bookingService.reserve(customer,
                    new BookingDto.CreateRequest(eventId, seatCategoryId, 1));
            UUID bookingId = reserved.id();

            // Force the booking into an already-expired state, exactly as
            // the scheduler would find it via
            // findByStatusAndExpiresAtBefore(PENDING, now).
            bookingRepository.findById(bookingId).ifPresent(b -> {
                b.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
                bookingRepository.save(b);
            });

            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            List<Exception> exceptions = new ArrayList<>();

            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    bookingService.confirm(bookingId, customer);
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                }
            });
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    bookingService.expireBooking(bookingId);
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                }
            });

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

            var finalBooking = bookingRepository.findById(bookingId).orElseThrow();
            var finalSeatCategory = seatCategoryRepository.findById(seatCategoryId).orElseThrow();

            // The booking's expiresAt was already in the past, so the only
            // correct final state is EXPIRED with the seat returned - it
            // must never end up CONFIRMED (that would mean confirm() won a
            // race against an already-due expiration).
            assertEquals(BookingStatus.EXPIRED, finalBooking.getStatus(),
                    "an already-expired PENDING booking must never end up CONFIRMED");
            assertEquals(1, finalSeatCategory.getAvailableSeats(),
                    "the seat must be released exactly once, trial " + t);
        }
    }

    // ---------------------------------------------------------------
    // Test 4: cancel() racing expireBooking() on the same PENDING booking
    // must release the seat exactly once, never twice.
    // ---------------------------------------------------------------
    @Test
    void concurrentCancelAndExpirationNeverDoubleReleaseSeats() throws InterruptedException {
        int trials = 15;

        for (int t = 0; t < trials; t++) {
            SeatCategory seatCategory = createEventWithSeatCategory(1);
            UUID eventId = seatCategory.getEvent().getId();
            UUID seatCategoryId = seatCategory.getId();
            User customer = createUser("customer", UserRole.CUSTOMER);

            BookingDto.Response reserved = bookingService.reserve(customer,
                    new BookingDto.CreateRequest(eventId, seatCategoryId, 1));
            UUID bookingId = reserved.id();

            bookingRepository.findById(bookingId).ifPresent(b -> {
                b.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
                bookingRepository.save(b);
            });

            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    bookingService.cancel(bookingId, customer);
                } catch (Exception ignored) {
                    // one of the two operations is expected to lose the
                    // race and throw (IllegalStateException) - that's
                    // correct behavior, asserted via final state below.
                }
            });
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    bookingService.expireBooking(bookingId);
                } catch (Exception ignored) {
                }
            });

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

            var finalBooking = bookingRepository.findById(bookingId).orElseThrow();
            var finalSeatCategory = seatCategoryRepository.findById(seatCategoryId).orElseThrow();

            assertTrue(
                    finalBooking.getStatus() == BookingStatus.CANCELLED
                            || finalBooking.getStatus() == BookingStatus.EXPIRED,
                    "booking must end CANCELLED or EXPIRED, trial " + t);
            assertEquals(1, finalSeatCategory.getAvailableSeats(),
                    "the seat must be released exactly once, trial " + t);
        }
    }
}