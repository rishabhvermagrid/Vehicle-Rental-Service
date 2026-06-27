package com.rental.booking.exception;

public class VehicleServiceUnavailableException extends RuntimeException {
    public VehicleServiceUnavailableException() {
        super("Vehicle service is currently unavailable. Please try again later.");
    }
}
