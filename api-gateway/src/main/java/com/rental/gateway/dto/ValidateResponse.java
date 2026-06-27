package com.rental.gateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Maps the body of GET /auth/validate from auth-service.
 * Example: { "success": true, "message": "Session is valid", "data": true }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidateResponse {
    private boolean success;
    private Boolean data;   // true = session alive, false = expired/invalid
}
