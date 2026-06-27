package com.rental.vehicle.exception;

public class DuplicateRegistrationNumberException extends RuntimeException {
    public DuplicateRegistrationNumberException(String registrationNumber) {
        super("Vehicle with registration number '" + registrationNumber + "' already exists");
    }
}
