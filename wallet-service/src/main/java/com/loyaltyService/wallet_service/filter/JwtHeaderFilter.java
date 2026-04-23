package com.loyaltyService.wallet_service.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
@Order(1)
public class JwtHeaderFilter implements Filter {

    // ✅ Public endpoints (no auth required)
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator",
            "/api/wallet/internal"
    );

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI();

        // ✅ Check if request is public
        boolean isPublic = PUBLIC_PATHS.stream()
                .anyMatch(path::startsWith);

        if (isPublic) {
            chain.doFilter(req, res);
            return;
        }

        // 🔐 Read headers from API Gateway
        String userId = request.getHeader("X-User-Id");
        String role   = request.getHeader("X-User-Role");

        // ❌ Block if missing
        if (userId == null || userId.isBlank()) {
            log.warn("Unauthorized request to {} - missing X-User-Id", path);

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Unauthorized — missing identity header\"}"
            );
            return;
        }

        // ✅ Log request (optional)
        log.debug("Wallet request → userId={}, role={}, path={}", userId, role, path);

        // 👉 Continue request
        chain.doFilter(req, res);
    }
}