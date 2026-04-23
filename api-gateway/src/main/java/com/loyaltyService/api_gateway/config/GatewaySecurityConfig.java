package com.loyaltyService.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Security configuration for the API Gateway (WebFlux).
 *
 * The gateway does NOT use Spring Security for JWT validation —
 * that is handled by the custom JwtAuthenticationFilter (GatewayFilter).
 *
 * This config:
 *   - Disables CSRF (stateless API, no sessions)
 *   - Disables Spring Security's default auth (we do it ourselves)
 *   - Configures CORS globally
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    private static final List<String> ALLOWED_ORIGIN_PATTERNS = List.of(
            "http://localhost:3001",
            "http://127.0.0.1:[*]"
    );
    private static final List<String> ALLOWED_HEADERS = List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "X-User-Id",
            "X-Userid",
            "X-User-Role",
            "X-User-Email",
            "ngrok-skip-browser-warning"
    );

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            // All actual auth is done in JwtAuthenticationFilter (GatewayFilter)
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll()
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration());
        return source;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebFilter corsResponseHeaderFilter() {
        return (exchange, chain) -> {
            String origin = exchange.getRequest().getHeaders().getOrigin();

            if (isAllowedOrigin(origin)) {
                applyCorsHeaders(exchange, origin);
                exchange.getResponse().beforeCommit(() -> {
                    applyCorsHeaders(exchange, origin);
                    return Mono.empty();
                });
            }

            if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
                exchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
                return Mono.empty();
            }

            return chain.filter(exchange);
        };
    }

    private boolean isAllowedOrigin(String origin) {
        return origin != null && corsConfiguration().checkOrigin(origin) != null;
    }

    private CorsConfiguration corsConfiguration() {
        CorsConfiguration config = new CorsConfiguration();

        // In production, replace localhost patterns with your actual frontend domain(s).
        config.setAllowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(ALLOWED_HEADERS);
        config.setExposedHeaders(List.of("Authorization", "X-User-Id", "X-User-Role", "X-User-Email"));
        config.setAllowCredentials(false); // set true if using cookies; false for JWT headers
        config.setMaxAge(3600L);
        return config;
    }

    private void applyCorsHeaders(ServerWebExchange exchange, String origin) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.setAccessControlAllowOrigin(origin);
        headers.setAccessControlAllowMethods(List.of(
                HttpMethod.GET,
                HttpMethod.POST,
                HttpMethod.PUT,
                HttpMethod.DELETE,
                HttpMethod.OPTIONS,
                HttpMethod.PATCH
        ));
        headers.setAccessControlAllowHeaders(ALLOWED_HEADERS);
        headers.setAccessControlExposeHeaders(List.of("Authorization", "X-User-Id", "X-User-Role", "X-User-Email"));
        headers.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        headers.add(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
        headers.add(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    }
}
