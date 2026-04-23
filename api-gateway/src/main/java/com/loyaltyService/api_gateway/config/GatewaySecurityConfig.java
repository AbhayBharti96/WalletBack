package com.loyaltyService.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

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
        CorsConfiguration config = new CorsConfiguration();

        // In production, replace "*" with your actual frontend domain(s)
        config.setAllowedOriginPatterns(List.of("http://localhost:5173/","*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "X-User-Id", "X-User-Role"));
        config.setAllowCredentials(false); // set true if using cookies; false for JWT headers
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
