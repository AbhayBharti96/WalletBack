package com.loyaltyService.auth_service.service;

import com.loyaltyService.auth_service.model.OtpStore;

public interface OtpService {
	String generateAndSaveOtpForEmail(String email, OtpStore.OtpType type);
	void verifyEmailOtp(String email, String otp, OtpStore.OtpType type);
	int getOtpExpiryMinutes();
}
