package com.loyaltyService.auth_service.service;

import com.loyaltyService.auth_service.dto.AuthDto;

public interface AuthService {
	AuthDto.UserProfile signup(AuthDto.SignupRequest request);
	void updateProfile(Long userId, String name, String phone);
	AuthDto.AuthResponse loginWithEmailPassword(AuthDto.LoginRequest request);
	AuthDto.AuthResponse loginWithPhonePassword(AuthDto.PhoneLoginRequest request);
	AuthDto.OtpSentResponse sendLoginOtp(AuthDto.SendOtpRequest request);
	AuthDto.AuthResponse verifyLoginOtp(AuthDto.VerifyOtpRequest request);
	AuthDto.OtpSentResponse sendPasswordResetOtp(AuthDto.ForgotPasswordOtpRequest request);
	AuthDto.PasswordResetTokenResponse verifyPasswordResetOtp(
            AuthDto.ForgotPasswordVerifyRequest request);
	AuthDto.SuccessResponse resetPassword(AuthDto.ResetPasswordRequest request);
	AuthDto.AuthResponse refreshToken(AuthDto.RefreshTokenRequest request);
	void updateStatus(Long userId, String status);
	void updateRole(Long userId, String role);
}
