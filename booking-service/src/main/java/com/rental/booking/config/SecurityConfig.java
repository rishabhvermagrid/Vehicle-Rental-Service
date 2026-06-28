package com.rental.booking.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderAuthenticationFilter headerAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Actuator
                .requestMatchers("/actuator/**").permitAll()

                // Create booking — all authenticated roles
                .requestMatchers(HttpMethod.POST, "/api/bookings")
                        .hasAnyRole("ADMIN", "STAFF", "CUSTOMER")

                // My bookings — CUSTOMER only
                .requestMatchers(HttpMethod.GET, "/api/bookings/my")
                        .hasRole("CUSTOMER")

                // All bookings — ADMIN and STAFF only
                .requestMatchers(HttpMethod.GET, "/api/bookings")
                        .hasAnyRole("ADMIN", "STAFF")

                // Get booking by id — all authenticated roles (service enforces ownership for CUSTOMER)
                .requestMatchers(HttpMethod.GET, "/api/bookings/**")
                        .hasAnyRole("ADMIN", "STAFF", "CUSTOMER")

                // Confirm booking — ADMIN and STAFF only
                .requestMatchers(HttpMethod.PATCH, "/api/bookings/*/confirm")
                        .hasAnyRole("ADMIN", "STAFF")

                // Cancel booking — all authenticated roles (service enforces ownership for CUSTOMER)
                .requestMatchers(HttpMethod.PATCH, "/api/bookings/*/cancel")
                        .hasAnyRole("ADMIN", "STAFF", "CUSTOMER")

                // Complete booking — ADMIN and STAFF only
                .requestMatchers(HttpMethod.PATCH, "/api/bookings/*/complete")
                        .hasAnyRole("ADMIN", "STAFF")

                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((request, response, denied) ->
                        response.sendError(HttpStatus.FORBIDDEN.value(), "Access Denied"))
            );

        return http.build();
    }
}
