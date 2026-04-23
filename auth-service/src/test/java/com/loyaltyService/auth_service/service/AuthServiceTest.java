package com.loyaltyService.auth_service.service;

import com.loyaltyService.auth_service.client.UserServiceClient;
import com.loyaltyService.auth_service.dto.AuthDto;
import com.loyaltyService.auth_service.exception.AuthException;
import com.loyaltyService.auth_service.model.OtpStore;
import com.loyaltyService.auth_service.model.RefreshToken;
import com.loyaltyService.auth_service.model.User;
import com.loyaltyService.auth_service.repository.UserRepository;
import com.loyaltyService.auth_service.security.JwtTokenProvider;
import com.loyaltyService.auth_service.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private EmailService emailService;
    @Mock
    private OtpService otpService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void clearResetTokens() {
        @SuppressWarnings("unchecked")
        Map<String, String> passwordResetTokens =
                (Map<String, String>) ReflectionTestUtils.getField(authService, "passwordResetTokens");
        passwordResetTokens.clear();
    }

    @Test
    void signupCreatesUserAndCallsUserService() {
        AuthDto.SignupRequest request = AuthDto.SignupRequest.builder()
                .fullName("Test User")
                .email("TEST@EXAMPLE.COM")
                .phone("9999999999")
                .password("Password@1")
                .build();
        User saved = user(1L, "test@example.com", "9999999999");

        when(userRepository.existsByEmail("TEST@EXAMPLE.COM")).thenReturn(false);
        when(userRepository.existsByPhone("9999999999")).thenReturn(false);
        when(passwordEncoder.encode("Password@1")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        AuthDto.UserProfile response = authService.signup(request);

        assertEquals(1L, response.getId());
        assertEquals("test@example.com", response.getEmail());
        verify(userRepository).save(argThat(user ->
                user.getFullName().equals("Test User")
                        && user.getEmail().equals("test@example.com")
                        && user.getPassword().equals("encoded")
                        && user.getRole() == User.Role.USER));
        verify(userServiceClient).createUser(new UserServiceClient.CreateUserRequest(
                1L, "Test User", "test@example.com", "9999999999", User.Role.USER));
    }

    @Test
    void signupThrowsWhenEmailExists() {
        AuthDto.SignupRequest request = AuthDto.SignupRequest.builder()
                .email("test@example.com")
                .phone("9999999999")
                .password("Password@1")
                .fullName("Test")
                .build();
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        AuthException exception = assertThrows(AuthException.class, () -> authService.signup(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void signupThrowsWhenPhoneExists() {
        AuthDto.SignupRequest request = AuthDto.SignupRequest.builder()
                .email("test@example.com")
                .phone("9999999999")
                .password("Password@1")
                .fullName("Test")
                .build();
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("9999999999")).thenReturn(true);

        AuthException exception = assertThrows(AuthException.class, () -> authService.signup(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void updateProfileUpdatesOnlyProvidedFields() {
        User user = user(1L, "test@example.com", "9999999999");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        authService.updateProfile(1L, "Updated Name", null);

        assertEquals("Updated Name", user.getFullName());
        assertEquals("9999999999", user.getPhone());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfileThrowsWhenUserMissing() {
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.updateProfile(10L, "Name", "9999999999"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void updateRolePersistsRoleAndRevokesRefreshTokens() {
        User user = user(1L, "test@example.com", "9999999999");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        authService.updateRole(1L, "ADMIN");

        assertEquals(User.Role.ADMIN, user.getRole());
        verify(userRepository).save(user);
        verify(refreshTokenService).revokeAllUserTokens(user);
    }

    @Test
    void updateRoleThrowsWhenRoleInvalid() {
        User user = user(1L, "test@example.com", "9999999999");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.updateRole(1L, "SUPER_ADMIN"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginWithEmailPasswordAuthenticatesAndReturnsTokens() {
        AuthDto.LoginRequest request = AuthDto.LoginRequest.builder()
                .email("TEST@EXAMPLE.COM")
                .password("Password@1")
                .build();
        User user = user(1L, "test@example.com", "9999999999");
        RefreshToken refreshToken = refreshToken(user, "refresh-token");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        AuthDto.AuthResponse response = authService.loginWithEmailPassword(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("TEST@EXAMPLE.COM", "Password@1"));
    }

    @Test
    void loginWithEmailPasswordThrowsWhenUserMissingAfterAuthentication() {
        AuthDto.LoginRequest request = AuthDto.LoginRequest.builder()
                .email("missing@example.com")
                .password("Password@1")
                .build();
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.loginWithEmailPassword(request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void loginWithPhonePasswordAuthenticatesUsingEmailPrincipal() {
        AuthDto.PhoneLoginRequest request = AuthDto.PhoneLoginRequest.builder()
                .phone("9999999999")
                .password("Password@1")
                .build();
        User user = user(2L, "phone@example.com", "9999999999");
        when(userRepository.findByPhone("9999999999")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("access");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken(user, "refresh"));

        AuthDto.AuthResponse response = authService.loginWithPhonePassword(request);

        assertEquals("access", response.getAccessToken());
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("phone@example.com", "Password@1"));
    }

    @Test
    void loginWithPhonePasswordThrowsWhenPhoneMissing() {
        when(userRepository.findByPhone("000")).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.loginWithPhonePassword(AuthDto.PhoneLoginRequest.builder()
                        .phone("000")
                        .password("Password@1")
                        .build()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void sendLoginOtpNormalizesEmailAndSendsOtp() {
        User user = user(3L, "user@example.com", "9999999999");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(otpService.generateAndSaveOtpForEmail("user@example.com", OtpStore.OtpType.LOGIN)).thenReturn("123456");
        when(otpService.getOtpExpiryMinutes()).thenReturn(5);

        AuthDto.OtpSentResponse response = authService.sendLoginOtp(
                AuthDto.SendOtpRequest.builder().email("USER@EXAMPLE.COM").build());

        assertEquals("user@example.com", response.getEmail());
        assertEquals(5, response.getExpiryMinutes());
        verify(emailService).sendOtp("user@example.com", "123456");
    }

    @Test
    void verifyLoginOtpDelegatesAndBuildsTokens() {
        User user = user(4L, "user@example.com", "9999999999");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("access");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken(user, "refresh"));

        AuthDto.AuthResponse response = authService.verifyLoginOtp(
                AuthDto.VerifyOtpRequest.builder().email("USER@EXAMPLE.COM").otp("123456").build());

        assertEquals("access", response.getAccessToken());
        verify(otpService).verifyEmailOtp("user@example.com", "123456", OtpStore.OtpType.LOGIN);
    }

    @Test
    void sendPasswordResetOtpSendsOtpForExistingUser() {
        User user = user(5L, "user@example.com", "9999999999");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(otpService.generateAndSaveOtpForEmail("User@Example.com", OtpStore.OtpType.PASSWORD_RESET))
                .thenReturn("654321");
        when(otpService.getOtpExpiryMinutes()).thenReturn(5);

        AuthDto.OtpSentResponse response = authService.sendPasswordResetOtp(
                AuthDto.ForgotPasswordOtpRequest.builder().email("User@Example.com").build());

        assertEquals("User@Example.com", response.getEmail());
        verify(emailService).sendOtp("User@Example.com", "654321");
    }

    @Test
    void verifyPasswordResetOtpStoresResetToken() {
        AuthDto.PasswordResetTokenResponse response = authService.verifyPasswordResetOtp(
                AuthDto.ForgotPasswordVerifyRequest.builder()
                        .email("user@example.com")
                        .otp("123456")
                        .build());

        assertNotNull(response.getResetToken());
        verify(otpService).verifyEmailOtp("user@example.com", "123456", OtpStore.OtpType.PASSWORD_RESET);
        @SuppressWarnings("unchecked")
        Map<String, String> passwordResetTokens =
                (Map<String, String>) ReflectionTestUtils.getField(authService, "passwordResetTokens");
        assertEquals("user@example.com", passwordResetTokens.get(response.getResetToken()));
    }

    @Test
    void resetPasswordUpdatesPasswordRevokesTokensAndRemovesResetToken() {
        User user = user(6L, "user@example.com", "9999999999");
        @SuppressWarnings("unchecked")
        Map<String, String> passwordResetTokens =
                (Map<String, String>) ReflectionTestUtils.getField(authService, "passwordResetTokens");
        passwordResetTokens.put("reset-token", "user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassword@1")).thenReturn("encoded-password");

        AuthDto.SuccessResponse response = authService.resetPassword(
                AuthDto.ResetPasswordRequest.builder()
                        .resetToken("reset-token")
                        .newPassword("NewPassword@1")
                        .build());

        assertEquals("Password reset successfully. Please login with your new password.", response.getMessage());
        assertEquals("encoded-password", user.getPassword());
        verify(userRepository).save(user);
        verify(refreshTokenService).revokeAllUserTokens(user);
        assertFalse(passwordResetTokens.containsKey("reset-token"));
    }

    @Test
    void resetPasswordThrowsWhenResetTokenInvalid() {
        AuthException exception = assertThrows(AuthException.class,
                () -> authService.resetPassword(AuthDto.ResetPasswordRequest.builder()
                        .resetToken("bad-token")
                        .newPassword("NewPassword@1")
                        .build()));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void resetPasswordThrowsWhenMappedUserMissing() {
        @SuppressWarnings("unchecked")
        Map<String, String> passwordResetTokens =
                (Map<String, String>) ReflectionTestUtils.getField(authService, "passwordResetTokens");
        passwordResetTokens.put("reset-token", "missing@example.com");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.resetPassword(AuthDto.ResetPasswordRequest.builder()
                        .resetToken("reset-token")
                        .newPassword("NewPassword@1")
                        .build()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void refreshTokenValidatesRevokesOldTokenAndBuildsNewTokens() {
        User user = user(7L, "user@example.com", "9999999999");
        RefreshToken oldToken = refreshToken(user, "old-token");
        RefreshToken newToken = refreshToken(user, "new-token");
        when(refreshTokenService.validateRefreshToken("old-token")).thenReturn(oldToken);
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("access");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(newToken);

        AuthDto.AuthResponse response = authService.refreshToken(
                AuthDto.RefreshTokenRequest.builder().refreshToken("old-token").build());

        assertEquals("new-token", response.getRefreshToken());
        verify(refreshTokenService).revokeRefreshToken("old-token");
    }

    private User user(Long id, String email, String phone) {
        return User.builder()
                .id(id)
                .fullName("Test User")
                .email(email)
                .phone(phone)
                .password("encoded")
                .role(User.Role.USER)
                .build();
    }

    private RefreshToken refreshToken(User user, String token) {
        return RefreshToken.builder()
                .id(1L)
                .user(user)
                .token(token)
                .expiryDate(Instant.now().plusSeconds(300))
                .isRevoked(false)
                .build();
    }
}
