package com.loyaltyService.wallet_service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyService.wallet_service.service.KafkaProducerService;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerServiceImpl implements KafkaProducerService{

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void send(String topic, Object message) {
        try {
            kafkaTemplate.send(topic, message)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            System.out.println("✅ Message sent: " + message);
                        } else {
                            System.out.println("❌ Failed: " + ex.getMessage());
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}