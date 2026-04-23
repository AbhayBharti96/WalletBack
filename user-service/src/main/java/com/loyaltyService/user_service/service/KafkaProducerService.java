package com.loyaltyService.user_service.service;

public interface KafkaProducerService {
    void send(String topic, Object message);
}
