package com.loyaltyService.reward_service.service;

public interface KafkaProducerService {
	void send(String topic, Object message);
}
