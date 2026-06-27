package com.rental.vehicle.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rental.vehicle.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderAuthenticationFilter headerAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless: identity comes from headers per request, no server-side session needed
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Insert our header-based auth filter before Spring's own form-login filter
            .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            .authorizeHttpRequests(auth -> auth
                    // Actuator health check is open
                    .requestMatchers("/actuator/**").permitAll()

                    // POST /vehicles → ADMIN only
                    .requestMatchers(HttpMethod.POST, "/vehicles").hasRole("ADMIN")

                    // GET /vehicles (all) → ADMIN, STAFF
                    .requestMatchers(HttpMethod.GET, "/vehicles").hasAnyRole("ADMIN", "STAFF")

                    // GET /vehicles/available → ADMIN, STAFF, CUSTOMER
                    // Must be declared BEFORE /vehicles/{id} to avoid path conflict
                    .requestMatchers(HttpMethod.GET, "/vehicles/available").hasAnyRole("ADMIN", "STAFF", "CUSTOMER")

                    // GET /vehicles/{id} → ADMIN, STAFF, CUSTOMER
                    .requestMatchers(HttpMethod.GET, "/vehicles/{id}").hasAnyRole("ADMIN", "STAFF", "CUSTOMER")

                    // PUT /vehicles/{id} → ADMIN only
                    .requestMatchers(HttpMethod.PUT, "/vehicles/{id}").hasRole("ADMIN")

                    // DELETE /vehicles/{id} → ADMIN only
                    .requestMatchers(HttpMethod.DELETE, "/vehicles/{id}").hasRole("ADMIN")

                    // PATCH /vehicles/{id}/status → ADMIN, STAFF
                    .requestMatchers(HttpMethod.PATCH, "/vehicles/{id}/status").hasAnyRole("ADMIN", "STAFF")

                    // Deny everything else
                    .anyRequest().denyAll()
            )

            // Return JSON 401/403 instead of HTML redirect pages
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, e) -> {
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        objectMapper.writeValue(response.getWriter(),
                                ApiResponse.error("Authentication required. Provide X-User-Id and X-User-Role headers."));
                    })
                    .accessDeniedHandler((request, response, e) -> {
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        objectMapper.writeValue(response.getWriter(),
                                ApiResponse.error("Access denied. You do not have permission to perform this action."));
                    })
            );

        return http.build();
    }
}
