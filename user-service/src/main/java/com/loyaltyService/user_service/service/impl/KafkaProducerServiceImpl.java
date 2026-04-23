package com.loyaltyService.user_service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyService.user_service.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerServiceImpl implements KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void send(String topic, Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);

            kafkaTemplate.send(topic, json)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            System.out.println("✅ Message sent: " + json);
                        } else {
                            System.out.println("❌ Failed: " + ex.getMessage());
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
