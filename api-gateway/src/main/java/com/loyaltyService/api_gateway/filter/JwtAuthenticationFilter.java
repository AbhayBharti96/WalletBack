package com.loyaltyService.api_gateway.filter;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Gateway-level JWT Authentication Filter (WebFlux / reactive).
 *
 * Applied to all protected routes via application.yml:
 *   filters:
 *     - JwtAuthentication
 *
 * What it does:
 *   1. Extracts "Authorization: Bearer <token>" header
 *   2. Validates JWT signature + expiry via JwtValidator
 *   3. On success: mutates the request, forwarding identity headers
 *      (X-User-Id, X-User-Role, X-User-Email) to downstream services
 *   4. On failure: returns 401 JSON immediately — request never reaches service
 *
 * Downstream services trust X-User-Id/X-User-Role without re-validating JWT.
 * Security note: in production, ensure microservices are NOT directly reachable
 * from outside — all traffic must flow through this gateway.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter
        extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtValidator jwtValidator;

    public JwtAuthenticationFilter(JwtValidator jwtValidator) {
        super(Config.class);
        this.jwtValidator = jwtValidator;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            ServerHttpRequest request = exchange.getRequest();

            String path = request.getURI().getPath();
            if (path.contains("/internal/")) {
                log.debug("Skipping JWT for internal path: {}", path);
                return chain.filter(exchange);
            }

            // ── 1. Extract token ──────────────────────────────────────────────
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.debug("Missing or malformed Authorization header on path: {}",
                        request.getPath());
                return rejectWith(exchange, HttpStatus.UNAUTHORIZED,
                        "Authorization header missing or invalid");
            }

            String token = authHeader.substring(7).trim();

            // ── 2. Validate JWT ───────────────────────────────────────────────
            Claims claims;
            try {
                claims = jwtValidator.validateAndExtract(token);
            } catch (JwtValidator.JwtValidationException ex) {
                log.warn("JWT validation failed [{}]: {}", request.getPath(), ex.getMessage());
                return rejectWith(exchange, HttpStatus.UNAUTHORIZED, ex.getMessage());
            }

            // ── 3. Extract identity from claims ───────────────────────────────
            String userId = claims.getSubject();                          // Long as String
            String role   = claims.get("role",  String.class);
            String email  = claims.get("email", String.class);

            if (userId == null || userId.isBlank()) {
                return rejectWith(exchange, HttpStatus.UNAUTHORIZED,
                        "Token missing subject (userId)");
            }

            log.debug("JWT OK — userId={}, role={}, path={}", userId, role, request.getPath());

            // ── 4. Forward identity as headers to downstream services ─────────
            ServerHttpRequest mutated = request.mutate()
                    .header("X-User-Id",    userId)
                    .header("X-User-Role",  role   != null ? role  : "")
                    .header("X-User-Email", email  != null ? email : "")
                    // Prevent clients from spoofing these headers
                    .headers(h -> {
                        h.remove("X-Forwarded-User-Id");
                        h.remove("X-Forwarded-User-Role");
                    })
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Mono<Void> rejectWith(ServerWebExchange exchange,
                                   HttpStatus status,
                                   String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"success\":false,\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                status.value(), status.getReasonPhrase(), message);

        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    /** No per-route configuration needed for this filter. */
    public static class Config {}
}
