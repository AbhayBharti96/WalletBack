package com.loyaltyService.auth_service.service.impl;

import com.loyaltyService.auth_service.exception.AuthException;
import com.loyaltyService.auth_service.model.RefreshToken;
import com.loyaltyService.auth_service.model.User;
import com.loyaltyService.auth_service.repository.RefreshTokenRepository;
import com.loyaltyService.auth_service.service.RefreshTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;  // milliseconds

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Revoke all existing tokens for user (single active session per user)
        refreshTokenRepository.revokeAllUserTokens(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpiration))
                .isRevoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthException("Refresh token not found", HttpStatus.UNAUTHORIZED));

        if (refreshToken.isRevoked()) {
            throw new AuthException("Refresh token has been revoked", HttpStatus.UNAUTHORIZED);
        }

        if (refreshToken.isExpired()) {
            throw new AuthException("Refresh token has expired. Please login again.", HttpStatus.UNAUTHORIZED);
        }
        return refreshToken;
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllUserTokens(user);
    }

    @Override
    @Scheduled(fixedDelayString = "PT1H")  // every hour
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredAndRevokedTokens(Instant.now());
        log.info("Expired/revoked refresh tokens cleaned up");
    }
    
    @Override
    @Transactional
    public void revokeToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        if (refreshToken.isRevoked()) {
            return;
        }
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }
}
