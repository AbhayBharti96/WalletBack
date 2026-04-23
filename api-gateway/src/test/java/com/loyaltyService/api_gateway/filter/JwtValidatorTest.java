package com.loyaltyService.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtValidatorTest {

    private final JwtValidator jwtValidator = new JwtValidator();
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        secretKey = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);
        ReflectionTestUtils.setField(jwtValidator, "secret", Encoders.BASE64.encode(secretKey.getEncoded()));
    }

    @Test
    void validateAndExtractReturnsClaimsForValidToken() {
        String token = Jwts.builder()
                .subject("99")
                .claim("role", "USER")
                .claim("email", "user@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(secretKey)
                .compact();

        Claims claims = jwtValidator.validateAndExtract(token);

        assertEquals("99", claims.getSubject());
        assertEquals("USER", claims.get("role", String.class));
        assertEquals("user@example.com", claims.get("email", String.class));
    }

    @Test
    void validateAndExtractThrowsForExpiredToken() {
        String token = Jwts.builder()
                .subject("99")
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(secretKey)
                .compact();

        JwtValidator.JwtValidationException exception = assertThrows(
                JwtValidator.JwtValidationException.class,
                () -> jwtValidator.validateAndExtract(token)
        );

        assertEquals("Token has expired", exception.getMessage());
    }

    @Test
    void validateAndExtractThrowsForMalformedToken() {
        JwtValidator.JwtValidationException exception = assertThrows(
                JwtValidator.JwtValidationException.class,
                () -> jwtValidator.validateAndExtract("not-a-jwt")
        );

        assertEquals("Malformed token", exception.getMessage());
    }
}
