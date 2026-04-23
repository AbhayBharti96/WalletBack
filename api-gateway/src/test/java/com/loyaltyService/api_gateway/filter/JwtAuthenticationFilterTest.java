package com.loyaltyService.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtValidator jwtValidator;

    @Mock
    private GatewayFilterChain chain;

    @Test
    void internalPathSkipsValidation() {
        JwtAuthenticationFilter filterFactory = new JwtAuthenticationFilter(jwtValidator);
        GatewayFilter filter = filterFactory.apply(new JwtAuthenticationFilter.Config());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/internal/health").build()
        );

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(jwtValidator, never()).validateAndExtract(any());
    }

    @Test
    void missingAuthorizationHeaderReturnsUnauthorized() {
        JwtAuthenticationFilter filterFactory = new JwtAuthenticationFilter(jwtValidator);
        GatewayFilter filter = filterFactory.apply(new JwtAuthenticationFilter.Config());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/me").build()
        );

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertTrue(exchange.getResponse().getBodyAsString().block().contains("Authorization header missing or invalid"));
        verify(chain, never()).filter(any());
    }

    @Test
    void invalidTokenReturnsUnauthorized() {
        JwtAuthenticationFilter filterFactory = new JwtAuthenticationFilter(jwtValidator);
        GatewayFilter filter = filterFactory.apply(new JwtAuthenticationFilter.Config());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer bad-token")
                        .build()
        );

        when(jwtValidator.validateAndExtract("bad-token"))
                .thenThrow(new JwtValidator.JwtValidationException("Malformed token"));

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertTrue(exchange.getResponse().getBodyAsString().block().contains("Malformed token"));
        verify(chain, never()).filter(any());
    }

    @Test
    void validTokenAddsIdentityHeaders() {
        JwtAuthenticationFilter filterFactory = new JwtAuthenticationFilter(jwtValidator);
        GatewayFilter filter = filterFactory.apply(new JwtAuthenticationFilter.Config());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .header("X-Forwarded-User-Id", "spoofed")
                        .build()
        );
        Claims claims = Jwts.claims()
                .subject("42")
                .add("role", "ADMIN")
                .add("email", "admin@example.com")
                .build();

        when(jwtValidator.validateAndExtract("valid-token")).thenReturn(claims);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        ArgumentCaptor<org.springframework.web.server.ServerWebExchange> exchangeCaptor =
                ArgumentCaptor.forClass(org.springframework.web.server.ServerWebExchange.class);
        verify(chain).filter(exchangeCaptor.capture());
        org.springframework.http.server.reactive.ServerHttpRequest mutatedRequest =
                exchangeCaptor.getValue().getRequest();
        assertEquals("42", mutatedRequest.getHeaders().getFirst("X-User-Id"));
        assertEquals("ADMIN", mutatedRequest.getHeaders().getFirst("X-User-Role"));
        assertEquals("admin@example.com", mutatedRequest.getHeaders().getFirst("X-User-Email"));
        assertEquals(null, mutatedRequest.getHeaders().getFirst("X-Forwarded-User-Id"));
    }
}
