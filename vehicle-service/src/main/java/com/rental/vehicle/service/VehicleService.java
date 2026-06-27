package com.rental.vehicle.service;

import com.rental.vehicle.dto.request.CreateVehicleRequest;
import com.rental.vehicle.dto.request.UpdateVehicleRequest;
import com.rental.vehicle.dto.request.UpdateVehicleStatusRequest;
import com.rental.vehicle.dto.response.VehicleResponse;

import java.util.List;

public interface VehicleService {

    VehicleResponse createVehicle(CreateVehicleRequest request);

    List<VehicleResponse> getAllVehicles();

    VehicleResponse getVehicleById(Long id);

    VehicleResponse updateVehicle(Long id, UpdateVehicleRequest request);

    void deleteVehicle(Long id);

    List<VehicleResponse> getAvailableVehicles();

    VehicleResponse updateVehicleStatus(Long id, UpdateVehicleStatusRequest request);
}
