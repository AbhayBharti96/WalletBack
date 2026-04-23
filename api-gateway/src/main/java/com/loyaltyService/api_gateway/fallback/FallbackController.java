package com.loyaltyService.api_gateway.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Fallback controller invoked by Resilience4j Circuit Breaker
 * when a downstream service is unavailable or times out.
 *
 * Routes reference these via: fallbackUri: forward:/fallback/<service>
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authFallback() {
        log.warn("Auth service circuit breaker triggered");
        return Mono.just(serviceUnavailable("auth-service"));
    }

    @RequestMapping("/user")
    public Mono<ResponseEntity<Map<String, Object>>> userFallback() {
        log.warn("User service circuit breaker triggered");
        return Mono.just(serviceUnavailable("user-service"));
    }

    @RequestMapping("/wallet")
    public Mono<ResponseEntity<Map<String, Object>>> walletFallback() {
        log.warn("Wallet service circuit breaker triggered");
        return Mono.just(serviceUnavailable("wallet-service"));
    }

    @RequestMapping("/rewards")
    public Mono<ResponseEntity<Map<String, Object>>> rewardsFallback() {
        log.warn("Rewards service circuit breaker triggered");
        return Mono.just(serviceUnavailable("rewards-service"));
    }

    @RequestMapping("/admin")
    public Mono<ResponseEntity<Map<String, Object>>> adminFallback() {
        log.warn("Admin service circuit breaker triggered");
        return Mono.just(serviceUnavailable("admin-service"));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> serviceUnavailable(String service) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "success",   false,
                        "status",    503,
                        "error",     "Service Unavailable",
                        "message",   service + " is temporarily unavailable. Please try again shortly.",
                        "timestamp", LocalDateTime.now().toString()
                ));
    }
}
