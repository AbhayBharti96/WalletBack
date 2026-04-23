package com.loyaltyService.auth_service.service;

import com.loyaltyService.auth_service.exception.AuthException;
import com.loyaltyService.auth_service.model.RefreshToken;
import com.loyaltyService.auth_service.model.User;
import com.loyaltyService.auth_service.repository.RefreshTokenRepository;
import com.loyaltyService.auth_service.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", 60000L);
    }

    @Test
    void createRefreshTokenRevokesExistingAndSavesNewToken() {
        User user = user(1L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        verify(refreshTokenRepository).revokeAllUserTokens(user);
        verify(refreshTokenRepository).save(argThat(token ->
                token.getUser().equals(user)
                        && token.getToken() != null
                        && !token.isRevoked()));
        assertFalse(refreshToken.isRevoked());
    }

    @Test
    void validateRefreshTokenReturnsTokenWhenValid() {
        RefreshToken refreshToken = refreshToken(user(1L), "good", Instant.now().plusSeconds(60), false);
        when(refreshTokenRepository.findByToken("good")).thenReturn(Optional.of(refreshToken));

        RefreshToken validated = refreshTokenService.validateRefreshToken("good");

        assertEquals("good", validated.getToken());
    }

    @Test
    void validateRefreshTokenThrowsWhenMissing() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class,
                () -> refreshTokenService.validateRefreshToken("missing"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void validateRefreshTokenThrowsWhenRevoked() {
        when(refreshTokenRepository.findByToken("revoked"))
                .thenReturn(Optional.of(refreshToken(user(1L), "revoked", Instant.now().plusSeconds(60), true)));

        AuthException exception = assertThrows(AuthException.class,
                () -> refreshTokenService.validateRefreshToken("revoked"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void validateRefreshTokenThrowsWhenExpired() {
        when(refreshTokenRepository.findByToken("expired"))
                .thenReturn(Optional.of(refreshToken(user(1L), "expired", Instant.now().minusSeconds(60), false)));

        AuthException exception = assertThrows(AuthException.class,
                () -> refreshTokenService.validateRefreshToken("expired"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void revokeRefreshTokenMarksTokenRevokedWhenFound() {
        RefreshToken refreshToken = refreshToken(user(1L), "token", Instant.now().plusSeconds(60), false);
        when(refreshTokenRepository.findByToken("token")).thenReturn(Optional.of(refreshToken));

        refreshTokenService.revokeRefreshToken("token");

        assertEquals(true, refreshToken.isRevoked());
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    void revokeRefreshTokenDoesNothingWhenTokenMissing() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        refreshTokenService.revokeRefreshToken("missing");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revokeAllUserTokensDelegatesToRepository() {
        User user = user(1L);

        refreshTokenService.revokeAllUserTokens(user);

        verify(refreshTokenRepository).revokeAllUserTokens(user);
    }

    @Test
    void cleanupExpiredTokensDelegatesToRepository() {
        refreshTokenService.cleanupExpiredTokens();

        verify(refreshTokenRepository).deleteExpiredAndRevokedTokens(any(Instant.class));
    }

    @Test
    void revokeTokenMarksTokenRevoked() {
        RefreshToken refreshToken = refreshToken(user(1L), "token", Instant.now().plusSeconds(60), false);
        when(refreshTokenRepository.findByToken("token")).thenReturn(Optional.of(refreshToken));

        refreshTokenService.revokeToken("token");

        assertEquals(true, refreshToken.isRevoked());
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    void revokeTokenReturnsWhenAlreadyRevoked() {
        RefreshToken refreshToken = refreshToken(user(1L), "token", Instant.now().plusSeconds(60), true);
        when(refreshTokenRepository.findByToken("token")).thenReturn(Optional.of(refreshToken));

        refreshTokenService.revokeToken("token");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revokeTokenThrowsWhenTokenMissing() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> refreshTokenService.revokeToken("missing"));
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .fullName("Test User")
                .email("user@example.com")
                .phone("9999999999")
                .password("encoded")
                .role(User.Role.USER)
                .build();
    }

    private RefreshToken refreshToken(User user, String token, Instant expiryDate, boolean revoked) {
        return RefreshToken.builder()
                .id(1L)
                .user(user)
                .token(token)
                .expiryDate(expiryDate)
                .isRevoked(revoked)
                .build();
    }
}
