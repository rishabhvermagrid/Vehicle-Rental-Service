package com.rental.vehicle.dto.request;

import com.rental.vehicle.enums.VehicleStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateVehicleStatusRequest {

    @NotNull(message = "Status is required")
    private VehicleStatus status;
}
