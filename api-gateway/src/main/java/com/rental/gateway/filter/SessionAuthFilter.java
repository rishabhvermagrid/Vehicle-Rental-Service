package com.rental.gateway.filter;

import com.rental.gateway.dto.UserResponse;
import com.rental.gateway.dto.ValidateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Runs before every request is forwarded to a downstream service.
 *
 * Flow for protected routes (/api/**):
 *  1. Extract SESSION_ID cookie set by auth-service after login (name configured in SessionConfig)
 *  2. Call auth-service /auth/validate — returns true/false
 *  3. If invalid → 401 immediately, request never reaches vehicle-service
 *  4. Call auth-service /auth/me — returns user id + role
 *  5. Inject X-User-Id and X-User-Role headers into the forwarded request
 *
 * Auth-service routes (/auth/**, /admin/**) are passed through untouched —
 * auth-service manages its own Spring Security for those paths.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionAuthFilter implements GlobalFilter, Ordered {

    private final WebClient webClient;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    // Requests to these prefixes bypass gateway-level auth validation
    private static final List<String> BYPASS_PREFIXES = List.of("/auth/", "/admin/");

    @Override
    public int getOrder() {
        return -1; // Run before Spring Cloud Gateway's own filters
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isBypassPath(path)) {
            log.debug("Bypassing auth filter for path: {}", path);
            return chain.filter(exchange);
        }

        // Extract SESSION_ID cookie — name set by SessionConfig in auth-service
        var sessionCookie = exchange.getRequest().getCookies().getFirst("SESSION_ID");
        if (sessionCookie == null) {
            log.warn("Request to {} rejected: no SESSION_ID cookie", path);
            return writeError(exchange, HttpStatus.UNAUTHORIZED,
                    "No active session. Please login at POST /auth/login first.");
        }

        String cookieHeader = "SESSION_ID=" + sessionCookie.getValue();

        return validateSession(cookieHeader)
                .flatMap(valid -> {
                    if (!Boolean.TRUE.equals(valid)) {
                        log.warn("Request to {} rejected: session invalid or expired", path);
                        return writeError(exchange, HttpStatus.UNAUTHORIZED,
                                "Session expired or invalid. Please login again.");
                    }
                    return fetchUserInfo(cookieHeader)
                            .flatMap(userResponse -> {
                                var user = userResponse.getData();
                                if (user == null || user.getId() == null || user.getRole() == null) {
                                    log.error("auth/me returned incomplete user data");
                                    return writeError(exchange, HttpStatus.INTERNAL_SERVER_ERROR,
                                            "Could not resolve user identity.");
                                }

                                log.debug("Forwarding {} → userId={}, role={}", path, user.getId(), user.getRole());

                                ServerHttpRequest mutated = exchange.getRequest().mutate()
                                        .header("X-User-Id", String.valueOf(user.getId()))
                                        .header("X-User-Role", user.getRole())
                                        .build();

                                return chain.filter(exchange.mutate().request(mutated).build());
                            });
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.warn("Auth service returned {}: {}", e.getStatusCode(), e.getMessage());
                    return writeError(exchange, HttpStatus.UNAUTHORIZED, "Session invalid or expired.");
                })
                .onErrorResume(e -> {
                    log.error("Auth service unreachable: {}", e.getMessage());
                    return writeError(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                            "Authentication service is currently unavailable. Please try again later.");
                });
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Mono<Boolean> validateSession(String cookieHeader) {
        return webClient.get()
                .uri(authServiceUrl + "/auth/validate")
                .header(HttpHeaders.COOKIE, cookieHeader)
                .retrieve()
                .bodyToMono(ValidateResponse.class)
                .map(ValidateResponse::getData);
    }

    private Mono<UserResponse> fetchUserInfo(String cookieHeader) {
        return webClient.get()
                .uri(authServiceUrl + "/auth/me")
                .header(HttpHeaders.COOKIE, cookieHeader)
                .retrieve()
                .bodyToMono(UserResponse.class);
    }

    private boolean isBypassPath(String path) {
        return BYPASS_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"success\":false,\"message\":\"%s\",\"timestamp\":\"%s\"}",
                message, LocalDateTime.now()
        );
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
