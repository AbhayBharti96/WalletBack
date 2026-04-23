package com.loyaltyService.auth_service.dto;


import com.loyaltyService.auth_service.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

public class AuthDto {
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SignupRequest {
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
        private String fullName;
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone must be 10-15 digits")
        private String phone;
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must contain uppercase, lowercase, digit, and special character"
        )
        private String password;
    }
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
        @NotBlank(message = "Password is required")
        private String password;
    }
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SendOtpRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
    }
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class VerifyOtpRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "OTP is required")
        @Size(min = 4, max = 8, message = "Invalid OTP length")
        private String otp;
    }
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PhoneLoginRequest {
        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
        private String phone;

        @NotBlank(message = "Password is required")
        private String password;
    }
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ForgotPasswordOtpRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
    }
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ForgotPasswordVerifyRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "OTP is required")
        @Size(min = 4, max = 8, message = "Invalid OTP length")
        private String otp;
    }
    
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ResetPasswordRequest {
        @NotBlank(message = "Reset token is required")
        private String resetToken;  // short-lived token from verify-otp step
        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must contain uppercase, lowercase, digit, and special character"
        )
        private String newPassword;
    }
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private long expiresIn;      // seconds
        private UserProfile user;
    }
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OtpSentResponse {
        private String message;
        private String email;
        private int expiryMinutes;
    }
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PasswordResetTokenResponse {
        private String message;
        private String resetToken;  // short-lived token to authorize password reset
        private int expiryMinutes;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SuccessResponse {
        private String message;
        private Object data;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UserProfile {
        private Long id;
        private String fullName;
        private String email;
        private String phone;
        private User.Role role;

        public static UserProfile from(User user) {
            return UserProfile.builder()
                    .id(user.getId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .role(user.getRole())
                    .build();
        }
    }
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private long timestamp;

        public static ErrorResponse of(int status, String error, String message) {
            return ErrorResponse.builder()
                    .status(status)
                    .error(error)
                    .message(message)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LogoutRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }
}