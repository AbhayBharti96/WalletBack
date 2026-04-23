package com.loyaltyService.api_gateway.fallback;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FallbackControllerTest {

    private final FallbackController controller = new FallbackController();
    private final WebTestClient webTestClient = WebTestClient.bindToController(controller).build();

    @ParameterizedTest
    @MethodSource("fallbackEndpoints")
    void fallbackEndpointsReturnServiceUnavailable(
            Supplier<reactor.core.publisher.Mono<ResponseEntity<Map<String, Object>>>> invocation,
            String serviceName
    ) {
        ResponseEntity<Map<String, Object>> response = invocation.get().block();

        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(false, response.getBody().get("success"));
        assertEquals(503, response.getBody().get("status"));
        assertEquals("Service Unavailable", response.getBody().get("error"));
        assertEquals(serviceName + " is temporarily unavailable. Please try again shortly.",
                response.getBody().get("message"));
        assertFalse(((String) response.getBody().get("timestamp")).isBlank());
    }

    @org.junit.jupiter.api.Test
    void authFallbackAcceptsPostRequests() {
        webTestClient
                .post()
                .uri("/fallback/auth")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.error").isEqualTo("Service Unavailable")
                .jsonPath("$.message").isEqualTo("auth-service is temporarily unavailable. Please try again shortly.");
    }

    private static Stream<Arguments> fallbackEndpoints() {
        FallbackController controller = new FallbackController();
        return Stream.of(
                Arguments.of((Supplier<reactor.core.publisher.Mono<ResponseEntity<Map<String, Object>>>>) controller::authFallback, "auth-service"),
                Arguments.of((Supplier<reactor.core.publisher.Mono<ResponseEntity<Map<String, Object>>>>) controller::userFallback, "user-service"),
                Arguments.of((Supplier<reactor.core.publisher.Mono<ResponseEntity<Map<String, Object>>>>) controller::walletFallback, "wallet-service"),
                Arguments.of((Supplier<reactor.core.publisher.Mono<ResponseEntity<Map<String, Object>>>>) controller::rewardsFallback, "rewards-service"),
                Arguments.of((Supplier<reactor.core.publisher.Mono<ResponseEntity<Map<String, Object>>>>) controller::adminFallback, "admin-service")
        );
    }
}
