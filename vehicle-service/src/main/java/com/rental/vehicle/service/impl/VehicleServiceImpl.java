package com.rental.vehicle.service.impl;

import com.rental.vehicle.dto.request.CreateVehicleRequest;
import com.rental.vehicle.dto.request.UpdateVehicleRequest;
import com.rental.vehicle.dto.request.UpdateVehicleStatusRequest;
import com.rental.vehicle.dto.response.VehicleResponse;
import com.rental.vehicle.entity.Vehicle;
import com.rental.vehicle.enums.VehicleStatus;
import com.rental.vehicle.exception.DuplicateRegistrationNumberException;
import com.rental.vehicle.exception.InvalidVehicleYearException;
import com.rental.vehicle.exception.VehicleNotFoundException;
import com.rental.vehicle.repository.VehicleRepository;
import com.rental.vehicle.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional
    public VehicleResponse createVehicle(CreateVehicleRequest request) {
        validateYear(request.getYear());

        if (vehicleRepository.existsByRegistrationNumber(request.getRegistrationNumber().toUpperCase())) {
            throw new DuplicateRegistrationNumberException(request.getRegistrationNumber());
        }

        Vehicle vehicle = Vehicle.builder()
                .brand(request.getBrand())
                .model(request.getModel())
                .year(request.getYear())
                .vehicleType(request.getVehicleType())
                .registrationNumber(request.getRegistrationNumber().toUpperCase())
                .pricePerDay(request.getPricePerDay())
                .status(VehicleStatus.AVAILABLE)
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle created: id={}, registration={}", saved.getId(), saved.getRegistrationNumber());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(Long id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(Long id, UpdateVehicleRequest request) {
        validateYear(request.getYear());

        Vehicle vehicle = findById(id);

        if (vehicleRepository.existsByRegistrationNumberAndIdNot(request.getRegistrationNumber().toUpperCase(), id)) {
            throw new DuplicateRegistrationNumberException(request.getRegistrationNumber());
        }

        vehicle.setBrand(request.getBrand());
        vehicle.setModel(request.getModel());
        vehicle.setYear(request.getYear());
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setRegistrationNumber(request.getRegistrationNumber().toUpperCase());
        vehicle.setPricePerDay(request.getPricePerDay());

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle updated: id={}", saved.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = findById(id);
        vehicleRepository.delete(vehicle);
        log.info("Vehicle deleted: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getAvailableVehicles() {
        return vehicleRepository.findByStatus(VehicleStatus.AVAILABLE).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicleStatus(Long id, UpdateVehicleStatusRequest request) {
        Vehicle vehicle = findById(id);
        vehicle.setStatus(request.getStatus());
        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle status updated: id={}, status={}", saved.getId(), saved.getStatus());
        return toResponse(saved);
    }

    // -------------------------------------------------------------------------

    private Vehicle findById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));
    }

    private void validateYear(int year) {
        int currentYear = Year.now().getValue();
        if (year > currentYear) {
            throw new InvalidVehicleYearException(year, currentYear);
        }
    }

    private VehicleResponse toResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .vehicleType(vehicle.getVehicleType())
                .registrationNumber(vehicle.getRegistrationNumber())
                .pricePerDay(vehicle.getPricePerDay())
                .status(vehicle.getStatus())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }
}
