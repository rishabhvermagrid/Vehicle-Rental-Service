package com.rental.vehicle.exception;

public class InvalidVehicleYearException extends RuntimeException {
    public InvalidVehicleYearException(int year, int currentYear) {
        super("Vehicle year " + year + " cannot be greater than the current year " + currentYear);
    }
}
