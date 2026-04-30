package com.loyaltyService.api_gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewaySecurityConfigTest {

    @Test
    void corsAllowsLegacyUserRoleHeaderVariants() {
        GatewaySecurityConfig config = new GatewaySecurityConfig();

        CorsConfiguration corsConfiguration =
                ReflectionTestUtils.invokeMethod(config, "corsConfiguration");

        assertNotNull(corsConfiguration);

        List<String> allowedHeaders = corsConfiguration.getAllowedHeaders();
        assertNotNull(allowedHeaders);
        assertTrue(allowedHeaders.contains("X-User-Role"));
        assertTrue(allowedHeaders.contains("X-UserRole"));
        assertTrue(allowedHeaders.contains("x-userrole"));
        assertTrue(allowedHeaders.contains("X-User-Email"));
        assertTrue(allowedHeaders.contains("X-UserEmail"));
        assertTrue(allowedHeaders.contains("x-useremail"));
    }
}
