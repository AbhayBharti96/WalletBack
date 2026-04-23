package com.loyaltyService.reward_service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.loyaltyService.reward_service.service.KafkaProducerService;

@Service
@RequiredArgsConstructor
public class KafkaProducerServiceImpl implements KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate; // String

    private final ObjectMapper objectMapper = new ObjectMapper(); // 👈 add this

    public void send(String topic, Object payload) {
        try {
            kafkaTemplate.send(topic, payload);

        } catch (Exception e) {
            throw new RuntimeException("Error serializing Kafka message", e);
        }
    }
}
