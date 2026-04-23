package com.loyaltyService.auth_service.controller;

import com.loyaltyService.auth_service.dto.AuthDto;
import com.loyaltyService.auth_service.service.AuthService;
import com.loyaltyService.auth_service.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    @PostMapping("/signup")
    public ResponseEntity<AuthDto.SuccessResponse> signup(
            @Valid @RequestBody AuthDto.SignupRequest request) {
        AuthDto.UserProfile profile = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                AuthDto.SuccessResponse.builder()
                        .message("Account created successfully")
                        .data(profile)
                        .build()
        );
    }
    @PostMapping("/login")
    public ResponseEntity<AuthDto.AuthResponse> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {
        return ResponseEntity.ok(authService.loginWithEmailPassword(request));
    }
    @PostMapping("/send-otp")
    public ResponseEntity<AuthDto.OtpSentResponse> sendOtp(
            @Valid @RequestBody AuthDto.SendOtpRequest request) {
        return ResponseEntity.ok(authService.sendLoginOtp(request));
    }
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthDto.AuthResponse> verifyOtp(
            @Valid @RequestBody AuthDto.VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyLoginOtp(request));
    }
    @PostMapping("/login/phone")
    public ResponseEntity<AuthDto.AuthResponse> loginWithPhone(
            @Valid @RequestBody AuthDto.PhoneLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithPhonePassword(request));
    }
    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<AuthDto.OtpSentResponse> forgotPasswordSendOtp(
            @Valid @RequestBody AuthDto.ForgotPasswordOtpRequest request) {
        return ResponseEntity.ok(authService.sendPasswordResetOtp(request));
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<AuthDto.PasswordResetTokenResponse> forgotPasswordVerifyOtp(
            @Valid @RequestBody AuthDto.ForgotPasswordVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyPasswordResetOtp(request));
    }
    @PostMapping("/reset-password")
    public ResponseEntity<AuthDto.SuccessResponse> resetPassword(
            @Valid @RequestBody AuthDto.ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
    @PostMapping("/refresh")
    public ResponseEntity<AuthDto.AuthResponse> refreshToken(
            @Valid @RequestBody AuthDto.RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running");
    }
    @PostMapping("/logout")
    public ResponseEntity<AuthDto.SuccessResponse> logout(
            @Valid @RequestBody AuthDto.LogoutRequest request) {
        refreshTokenService.revokeToken(request.getRefreshToken());
        return ResponseEntity.ok(
                AuthDto.SuccessResponse.builder()
                        .message("Logged out successfully")
                        .build()
        );
    }

    @PutMapping("/internal/update-profile")
    public ResponseEntity<Void> updateProfile(
            @RequestBody UpdateProfileRequest req) {
        authService.updateProfile(req.userId(), req.name(), req.phone());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/internal/status")
    public ResponseEntity<Void> updateStatus(@RequestBody StatusUpdateRequest req) {
        authService.updateStatus(req.userId(), req.status());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/internal/role")
    public ResponseEntity<Void> updateRole(@RequestBody RoleUpdateRequest req) {
        authService.updateRole(req.userId(), req.role());
        return ResponseEntity.ok().build();
    }

    record StatusUpdateRequest(Long userId, String status) {}

    record RoleUpdateRequest(Long userId, String role) {}

    record UpdateProfileRequest(Long userId, String name, String phone) {}
}
