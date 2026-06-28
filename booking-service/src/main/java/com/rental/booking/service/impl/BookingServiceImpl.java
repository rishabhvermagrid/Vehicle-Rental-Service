package com.rental.booking.service.impl;

import com.rental.booking.client.VehicleClient;
import com.rental.booking.dto.request.CreateBookingRequest;
import com.rental.booking.dto.response.BookingResponse;
import com.rental.booking.dto.response.VehicleResponse;
import com.rental.booking.entity.Booking;
import com.rental.booking.enums.BookingStatus;
import com.rental.booking.exception.*;
import com.rental.booking.repository.BookingRepository;
import com.rental.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final VehicleClient vehicleClient;

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, Long customerId, String customerRole) {
        // CUSTOMER must book for themselves only
        Long resolvedCustomerId = "CUSTOMER".equals(customerRole)
                ? customerId
                : (request.getCustomerId() != null ? request.getCustomerId() : customerId);

        // Validate dates
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new InvalidBookingException("End date must be after start date.");
        }

        // Fetch vehicle from vehicle-service
        VehicleResponse vehicle = vehicleClient.getVehicleById(request.getVehicleId());

        // Check vehicle is available
        if (!"AVAILABLE".equalsIgnoreCase(vehicle.getStatus())) {
            throw new VehicleNotAvailableException(
                    "Vehicle is not available for booking. Current status: " + vehicle.getStatus());
        }

        // Check no active booking overlaps (PENDING or CONFIRMED means it's taken)
        boolean activeExists = bookingRepository.existsByVehicleIdAndStatusIn(
                request.getVehicleId(),
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
        if (activeExists) {
            throw new VehicleNotAvailableException("Vehicle already has an active booking.");
        }

        // Calculate totals
        long totalDays = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        BigDecimal totalAmount = vehicle.getPricePerDay().multiply(BigDecimal.valueOf(totalDays));

        Booking booking = Booking.builder()
                .vehicleId(request.getVehicleId())
                .customerId(resolvedCustomerId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalDays((int) totalDays)
                .totalAmount(totalAmount)
                .build();

        booking = bookingRepository.save(booking);

        // Mark vehicle as BOOKED
        vehicleClient.updateVehicleStatus(request.getVehicleId(), "BOOKED");

        log.info("Booking created: id={}, vehicleId={}, customerId={}", booking.getId(),
                booking.getVehicleId(), booking.getCustomerId());

        return BookingResponse.from(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id, Long userId, String userRole) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));

        // CUSTOMER can only view their own bookings
        if ("CUSTOMER".equals(userRole) && !booking.getCustomerId().equals(userId)) {
            throw new AccessDeniedException("You do not have access to this booking.");
        }

        return BookingResponse.from(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(Long customerId) {
        return bookingRepository.findByCustomerId(customerId)
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long id, Long userId, String userRole) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));

        // CUSTOMER can only cancel their own bookings
        if ("CUSTOMER".equals(userRole) && !booking.getCustomerId().equals(userId)) {
            throw new AccessDeniedException("You can only cancel your own bookings.");
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingException(
                    "Cannot cancel a booking with status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);

        // Release vehicle back to AVAILABLE
        vehicleClient.updateVehicleStatus(booking.getVehicleId(), "AVAILABLE");

        log.info("Booking cancelled: id={}, vehicleId={}", booking.getId(), booking.getVehicleId());

        return BookingResponse.from(booking);
    }

    @Override
    @Transactional
    public BookingResponse completeBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingException(
                    "Only CONFIRMED bookings can be marked as completed. Current status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking = bookingRepository.save(booking);

        // Release vehicle back to AVAILABLE
        vehicleClient.updateVehicleStatus(booking.getVehicleId(), "AVAILABLE");

        log.info("Booking completed: id={}, vehicleId={}", booking.getId(), booking.getVehicleId());

        return BookingResponse.from(booking);
    }
}
