package com.rental.booking.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rental.booking.dto.response.VehicleResponse;
import com.rental.booking.exception.VehicleNotFoundException;
import com.rental.booking.exception.VehicleServiceUnavailableException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@Slf4j
public class VehicleClient {

    private final WebClient webClient;

    // booking-service acts as ADMIN for internal service-to-service calls
    // so it can both read vehicles and update their status
    private static final String INTERNAL_USER_ID   = "0";
    private static final String INTERNAL_USER_ROLE = "ADMIN";

    public VehicleClient(WebClient.Builder builder,
                         @Value("${vehicle.service.url}") String vehicleServiceUrl) {
        this.webClient = builder
                .baseUrl(vehicleServiceUrl)
                .defaultHeader("X-User-Id",   INTERNAL_USER_ID)
                .defaultHeader("X-User-Role", INTERNAL_USER_ROLE)
                .build();
    }

    // ── Called before booking is created ─────────────────────────────────────

    public VehicleResponse getVehicleById(Long vehicleId) {
        log.info("Fetching vehicle id={} from vehicle-service", vehicleId);
        try {
            VehicleApiResponse response = webClient.get()
                    .uri("/api/vehicles/{id}", vehicleId)
                    .retrieve()
                    .bodyToMono(VehicleApiResponse.class)
                    .block();

            if (response == null || response.getData() == null) {
                throw new VehicleNotFoundException(vehicleId);
            }
            return response.getData();

        } catch (WebClientResponseException.NotFound e) {
            throw new VehicleNotFoundException(vehicleId);
        } catch (VehicleNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("vehicle-service unreachable: {}", e.getMessage());
            throw new VehicleServiceUnavailableException();
        }
    }

    // ── Called after booking is created (set to BOOKED) ──────────────────────
    // ── Called on cancel/complete (set back to AVAILABLE) ────────────────────

    public void updateVehicleStatus(Long vehicleId, String status) {
        log.info("Updating vehicle id={} status to {}", vehicleId, status);
        try {
            webClient.patch()
                    .uri("/api/vehicles/{id}/status", vehicleId)
                    .bodyValue(new StatusUpdateRequest(status))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to update vehicle id={} status to {}: {}", vehicleId, status, e.getMessage());
            throw new VehicleServiceUnavailableException();
        }
    }

    // ── Private inner types ───────────────────────────────────────────────────

    // Wraps vehicle-service ApiResponse<VehicleResponse>
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class VehicleApiResponse {
        private boolean success;
        private VehicleResponse data;
    }

    // Request body for PATCH /api/vehicles/{id}/status
    private record StatusUpdateRequest(String status) {}
}
