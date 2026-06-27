package com.rental.gateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * The "data" field inside GET /auth/me response.
 * We only need id and role; the rest is ignored.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfo {
    private Long id;
    private String role;   // "ADMIN" | "STAFF" | "CUSTOMER"
}
