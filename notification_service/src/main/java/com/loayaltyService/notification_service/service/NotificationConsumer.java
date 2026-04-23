package com.loayaltyService.notification_service.service;

public interface NotificationConsumer {
	void walletEvents(String event);
	void paymentEvents(String event);
	void rewardEvents(String event);
	void kycEvents(String event);
}
