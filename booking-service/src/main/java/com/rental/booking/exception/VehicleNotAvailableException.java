package com.rental.booking.exception;

public class VehicleNotAvailableException extends RuntimeException {
    public VehicleNotAvailableException(Long vehicleId, String currentStatus) {
        super("Vehicle id " + vehicleId + " is not available for booking. Current status: " + currentStatus);
    }
}
