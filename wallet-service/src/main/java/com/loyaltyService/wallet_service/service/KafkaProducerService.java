package com.loyaltyService.wallet_service.service;

public interface KafkaProducerService {

	void send(String topic, Object message);
}
