package com.loyaltyService.api_gateway.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;

/**
 * Stateless JWT validator used by the gateway filter.
 * Parses the token, validates signature + expiry, and extracts claims.
 *
 * NOTE: Uses jjwt 0.11.x API (parseClaimsJws). If you later upgrade to
 * jjwt 0.12.x, switch to parseSignedClaims().
 */
@Slf4j
@Component
public class JwtValidator {

    @Value("${jwt.secret}")
    private String secret;

    /**
     * Validates the JWT token and returns its claims.
     *
     * @param token raw JWT string (without "Bearer " prefix)
     * @return parsed {@link Claims}
     * @throws JwtValidationException if token is invalid or expired
     */
    public Claims validateAndExtract(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ExpiredJwtException ex) {
            log.warn("JWT expired: {}", ex.getMessage());
            throw new JwtValidationException("Token has expired");
        } catch (SignatureException ex) {
            log.warn("Invalid JWT signature: {}", ex.getMessage());
            throw new JwtValidationException("Invalid token signature");
        } catch (MalformedJwtException ex) {
            log.warn("Malformed JWT: {}", ex.getMessage());
            throw new JwtValidationException("Malformed token");
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT: {}", ex.getMessage());
            throw new JwtValidationException("Unsupported token");
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims empty: {}", ex.getMessage());
            throw new JwtValidationException("Token is empty or null");
        }
    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }


    // ── Inner exception ───────────────────────────────────────────────────────
    public static class JwtValidationException extends RuntimeException {
        public JwtValidationException(String message) {
            super(message);
        }
    }
}
