package com.rental.vehicle.dto.response;

import com.rental.vehicle.enums.VehicleStatus;
import com.rental.vehicle.enums.VehicleType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class VehicleResponse {
    private Long id;
    private String brand;
    private String model;
    private Integer year;
    private VehicleType vehicleType;
    private String registrationNumber;
    private BigDecimal pricePerDay;
    private VehicleStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
