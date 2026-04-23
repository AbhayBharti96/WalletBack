package com.loyaltyService.auth_service.service;

public interface EmailService {
	void sendOtp(String recipientEmail, String otp);
}
