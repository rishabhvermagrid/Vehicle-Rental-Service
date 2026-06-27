package com.rental.vehicle.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads X-User-Id and X-User-Role injected by the API Gateway (which has already
 * validated the session against user-service / Redis) and populates the Spring
 * Security context so the rest of the filter chain can apply role-based rules.
 *
 * Role values from user-service: ADMIN | STAFF | CUSTOMER (plain enum names).
 * We prefix with ROLE_ to satisfy Spring Security's hasRole() convention.
 */
@Component
@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");

        if (userId != null && !userId.isBlank() && userRole != null && !userRole.isBlank()) {
            try {
                String authority = "ROLE_" + userRole.trim().toUpperCase();
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userId.trim(),
                        null,
                        List.of(new SimpleGrantedAuthority(authority))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Authenticated from headers: userId={}, role={}", userId, userRole);
            } catch (Exception e) {
                log.warn("Could not set authentication from headers: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}
