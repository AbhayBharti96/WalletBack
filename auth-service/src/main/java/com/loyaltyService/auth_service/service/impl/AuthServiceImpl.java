package com.loyaltyService.auth_service.service.impl;
import com.loyaltyService.auth_service.client.UserServiceClient;
import com.loyaltyService.auth_service.dto.AuthDto;
import com.loyaltyService.auth_service.exception.AuthException;
import com.loyaltyService.auth_service.exception.UserBlockedException;
import com.loyaltyService.auth_service.model.OtpStore;
import com.loyaltyService.auth_service.model.RefreshToken;
import com.loyaltyService.auth_service.model.User;
import com.loyaltyService.auth_service.repository.UserRepository;
import com.loyaltyService.auth_service.security.JwtTokenProvider;
import com.loyaltyService.auth_service.service.AuthService;
import com.loyaltyService.auth_service.service.EmailService;
import com.loyaltyService.auth_service.service.OtpService;
import com.loyaltyService.auth_service.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserServiceClient userServiceClient;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final OtpService otpService;
    private final AuthenticationManager authenticationManager;
    private final Map<String, String> passwordResetTokens = new ConcurrentHashMap<>();
    
    @Override
    @Transactional
    public AuthDto.UserProfile signup(AuthDto.SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email already registered", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new AuthException("Phone number already registered", HttpStatus.CONFLICT);
        }
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();
        User saved = userRepository.save(user);
        userServiceClient.createUser(new UserServiceClient.CreateUserRequest(
                saved.getId(),
                saved.getFullName(),
                saved.getEmail(),
                saved.getPhone(),
                saved.getRole()
        ));
        log.info("New user registered: id={}, email={}", saved.getId(), saved.getEmail());
        return AuthDto.UserProfile.from(saved);
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, String name, String phone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.NOT_FOUND));
        if (name  != null && !name.isBlank())  user.setFullName(name);
        if (phone != null && !phone.isBlank()) user.setPhone(phone);
        userRepository.save(user);
        log.info("Auth profile updated: userId={}", userId);
    }

    record UpdateProfileRequest(Long userId, String name, String phone) {}

    @Override
    @Transactional
    public AuthDto.AuthResponse loginWithEmailPassword(AuthDto.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.NOT_FOUND));
        if (!user.isActive()) {
            throw new UserBlockedException();
        }

        return buildAuthResponse(user);
    }
    @Override
    @Transactional
    public AuthDto.AuthResponse loginWithPhonePassword(AuthDto.PhoneLoginRequest request) {

        // Step 1: Find user by phone
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.NOT_FOUND));

        // Step 2: Authenticate using email as principal
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),  // MUST match what UserDetailsService returns
                        request.getPassword()
                )
        );

        if (!user.isActive()) {
            throw new UserBlockedException();
        }

        // Step 3: Return JWT response
        return buildAuthResponse(user);
    }
    @Override
    @Transactional
    public AuthDto.OtpSentResponse sendLoginOtp(AuthDto.SendOtpRequest request) {
        String email = request.getEmail().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(
                        "No account found with this email", HttpStatus.NOT_FOUND));

        if (!user.isActive()) {
            throw new UserBlockedException();
        }

        String otp = otpService.generateAndSaveOtpForEmail(email, OtpStore.OtpType.LOGIN);

        emailService.sendOtp(email, otp);

        return AuthDto.OtpSentResponse.builder()
                .message("OTP sent successfully")
                .email(email)
                .expiryMinutes(otpService.getOtpExpiryMinutes())
                .build();
    }
    @Override
    @Transactional
    public AuthDto.AuthResponse verifyLoginOtp(AuthDto.VerifyOtpRequest request) {
        String email = request.getEmail().toLowerCase(); // normalize first

        otpService.verifyEmailOtp(email, request.getOtp(), OtpStore.OtpType.LOGIN); //  lowercased email

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.NOT_FOUND));

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthDto.OtpSentResponse sendPasswordResetOtp(AuthDto.ForgotPasswordOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new AuthException(
                        "No account found with this email", HttpStatus.NOT_FOUND));

        if (!user.isActive()) {
            throw new UserBlockedException();
        }

        String otp = otpService.generateAndSaveOtpForEmail(request.getEmail(), OtpStore.OtpType.PASSWORD_RESET);

        emailService.sendOtp(request.getEmail(), otp);

        return AuthDto.OtpSentResponse.builder()
                .message("Password reset OTP sent to your email")
                .email(request.getEmail())
                .expiryMinutes(otpService.getOtpExpiryMinutes())
                .build();
    }

    @Override
    @Transactional
    public AuthDto.PasswordResetTokenResponse verifyPasswordResetOtp(
            AuthDto.ForgotPasswordVerifyRequest request) {

        otpService.verifyEmailOtp(request.getEmail(), request.getOtp(), OtpStore.OtpType.PASSWORD_RESET);

        String resetToken = java.util.UUID.randomUUID().toString();
        passwordResetTokens.put(resetToken, request.getEmail());

        scheduleResetTokenExpiry(resetToken);

        return AuthDto.PasswordResetTokenResponse.builder()
                .message("OTP verified. Use the reset token to set a new password.")
                .resetToken(resetToken)
                .expiryMinutes(10)
                .build();
    }
    @Override
    @Transactional
    public AuthDto.SuccessResponse resetPassword(AuthDto.ResetPasswordRequest request) {

        String email = passwordResetTokens.get(request.getResetToken());

        if (email == null) {
            throw new AuthException(
                    "Invalid or expired reset token. Please restart the password recovery flow.",
                    HttpStatus.BAD_REQUEST
            );
        }

        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.NOT_FOUND));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenService.revokeAllUserTokens(user);

        passwordResetTokens.remove(request.getResetToken());

        log.info("Password reset successful for user with email: {}", user.getEmail());

        return AuthDto.SuccessResponse.builder()
                .message("Password reset successfully. Please login with your new password.")
                .build();
    }


    @Override
    @Transactional
    public AuthDto.AuthResponse refreshToken(AuthDto.RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();

        if (!user.isActive()) {
            throw new UserBlockedException();
        }

        // Rotate refresh token (revoke old, issue new)
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public void updateStatus(Long userId, String status) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.NOT_FOUND));

        user.setActive("ACTIVE".equalsIgnoreCase(status));

        userRepository.save(user);

        log.info("User status updated from user-service: userId={}, status={}", userId, status);
    }

    @Override
    @Transactional
    public void updateRole(Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.NOT_FOUND));

        User.Role newRole;
        try {
            newRole = User.Role.valueOf(role.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new AuthException("Invalid role: " + role, HttpStatus.BAD_REQUEST);
        }

        user.setRole(newRole);
        userRepository.save(user);
        refreshTokenService.revokeAllUserTokens(user);

        log.info("User role updated from user-service: userId={}, role={}", userId, newRole);
    }


    private AuthDto.AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthDto.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(1800)
                .user(AuthDto.UserProfile.from(user))
                .build();
    }

    private void scheduleResetTokenExpiry(String token) {
        new Thread(() -> {
            try {
                Thread.sleep(10 * 60 * 1000L);
                passwordResetTokens.remove(token);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}
