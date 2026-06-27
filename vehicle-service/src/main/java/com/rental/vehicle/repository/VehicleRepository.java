package com.rental.vehicle.repository;

import com.rental.vehicle.entity.Vehicle;
import com.rental.vehicle.enums.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumberAndIdNot(String registrationNumber, Long id);

    List<Vehicle> findByStatus(VehicleStatus status);

    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);
}
