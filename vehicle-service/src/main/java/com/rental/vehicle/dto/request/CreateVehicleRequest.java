package com.rental.vehicle.dto.request;

import com.rental.vehicle.enums.VehicleType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Year;

@Data
public class CreateVehicleRequest {

    @NotBlank(message = "Brand is required")
    @Size(max = 100, message = "Brand must not exceed 100 characters")
    private String brand;

    @NotBlank(message = "Model is required")
    @Size(max = 100, message = "Model must not exceed 100 characters")
    private String model;

    @NotNull(message = "Year is required")
    @Min(value = 1886, message = "Year must be 1886 or later")
    private Integer year;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotBlank(message = "Registration number is required")
    @Size(max = 50, message = "Registration number must not exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Registration number must contain only uppercase letters, digits, and hyphens")
    private String registrationNumber;

    @NotNull(message = "Price per day is required")
    @DecimalMin(value = "0.01", message = "Price per day must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price per day format is invalid")
    private BigDecimal pricePerDay;
}
