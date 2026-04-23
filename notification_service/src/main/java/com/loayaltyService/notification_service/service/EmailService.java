package com.loayaltyService.notification_service.service;

public interface EmailService {
	void send(String to, String subject, String body);
	void sendHtml(String to, String subject, String body);
}
