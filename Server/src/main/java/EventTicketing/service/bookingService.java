package EventTicketing.service;

import EventTicketing.dto.bookingDto;
import EventTicketing.model.User;
import EventTicketing.repository.bookingRepository;
import EventTicketing.repository.eventRepository;
import EventTicketing.repository.seatCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class bookingService {

    private final bookingRepository bookingRepository;
    private final eventRepository eventRepository;
    private final seatCategoryRepository seatCategoryRepository;

    public bookingDto.Response reserve(User user, bookingDto.CreateRequest request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public bookingDto.Response confirm(UUID bookingId, User user) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public bookingDto.Response cancel(UUID bookingId, User user) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<bookingDto.Response> myBookings(User user) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}