package com.rental.gateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Maps the body of GET /auth/me from auth-service.
 * Example: { "success": true, "message": "User retrieved", "data": { "id": 1, "role": "ADMIN", ... } }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResponse {
    private boolean success;
    private UserInfo data;
}
