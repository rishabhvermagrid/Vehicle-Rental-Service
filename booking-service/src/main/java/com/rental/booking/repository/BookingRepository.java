package com.rental.booking.repository;

import com.rental.booking.entity.Booking;
import com.rental.booking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // GET /bookings/customer/{customerId}
    List<Booking> findByCustomerId(Long customerId);

    // check if a vehicle already has an active booking
    boolean existsByVehicleIdAndStatusIn(Long vehicleId, List<BookingStatus> statuses);
}
