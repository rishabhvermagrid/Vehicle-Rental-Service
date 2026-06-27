package com.rental.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VehicleResponse {

    private Long id;
    private String brand;
    private String model;
    private Integer year;
    private String vehicleType;
    private String registrationNumber;
    private BigDecimal pricePerDay;
    private String status;
}
