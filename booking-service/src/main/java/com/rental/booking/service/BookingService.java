package com.rental.booking.service;

import com.rental.booking.dto.request.CreateBookingRequest;
import com.rental.booking.dto.response.BookingResponse;

import java.util.List;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request, Long customerId, String customerRole);

    BookingResponse getBookingById(Long id, Long userId, String userRole);

    List<BookingResponse> getMyBookings(Long customerId);

    List<BookingResponse> getAllBookings();

    BookingResponse confirmBooking(Long id);

    BookingResponse cancelBooking(Long id, Long userId, String userRole);

    BookingResponse completeBooking(Long id);
}
