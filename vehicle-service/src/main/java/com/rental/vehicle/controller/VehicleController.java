package com.rental.vehicle.controller;

import com.rental.vehicle.dto.request.CreateVehicleRequest;
import com.rental.vehicle.dto.request.UpdateVehicleRequest;
import com.rental.vehicle.dto.request.UpdateVehicleStatusRequest;
import com.rental.vehicle.dto.response.ApiResponse;
import com.rental.vehicle.dto.response.VehicleResponse;
import com.rental.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    // Role enforcement is handled by SecurityConfig + HeaderAuthenticationFilter.
    // @AuthenticationPrincipal resolves to the X-User-Id value set by the filter (used for audit logging).

    /** POST /vehicles — ADMIN only */
    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody CreateVehicleRequest request) {

        VehicleResponse vehicle = vehicleService.createVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(vehicle, "Vehicle created successfully"));
    }

    /** GET /vehicles — ADMIN, STAFF */
    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getAllVehicles(
            @AuthenticationPrincipal String userId) {

        List<VehicleResponse> vehicles = vehicleService.getAllVehicles();
        return ResponseEntity.ok(ApiResponse.success(vehicles, "Vehicles retrieved successfully"));
    }

    /** GET /vehicles/available — ADMIN, STAFF, CUSTOMER */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getAvailableVehicles(
            @AuthenticationPrincipal String userId) {

        List<VehicleResponse> vehicles = vehicleService.getAvailableVehicles();
        return ResponseEntity.ok(ApiResponse.success(vehicles, "Available vehicles retrieved successfully"));
    }

    /** GET /vehicles/{id} — ADMIN, STAFF, CUSTOMER */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicleById(
            @AuthenticationPrincipal String userId,
            @PathVariable Long id) {

        VehicleResponse vehicle = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(ApiResponse.success(vehicle, "Vehicle retrieved successfully"));
    }

    /** PUT /vehicles/{id} — ADMIN only */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(
            @AuthenticationPrincipal String userId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleRequest request) {

        VehicleResponse vehicle = vehicleService.updateVehicle(id, request);
        return ResponseEntity.ok(ApiResponse.success(vehicle, "Vehicle updated successfully"));
    }

    /** DELETE /vehicles/{id} — ADMIN only */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(
            @AuthenticationPrincipal String userId,
            @PathVariable Long id) {

        vehicleService.deleteVehicle(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Vehicle deleted successfully"));
    }

    /** PATCH /vehicles/{id}/status — ADMIN, STAFF */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicleStatus(
            @AuthenticationPrincipal String userId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleStatusRequest request) {

        VehicleResponse vehicle = vehicleService.updateVehicleStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(vehicle, "Vehicle status updated successfully"));
    }
}
