package com.loyaltyService.reward_service.service;

import com.loyaltyService.reward_service.service.impl.KafkaProducerServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaProducerServiceImpl kafkaProducerService;

    @Test
    void sendDelegatesToKafkaTemplate() {
        when(kafkaTemplate.send(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        kafkaProducerService.send("reward-events", Map.of("userId", 1L));

        verify(kafkaTemplate).send("reward-events", Map.of("userId", 1L));
    }

    @Test
    void sendSwallowsCallbackFailure() {
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("kafka down"));
        when(kafkaTemplate.send(anyString(), any())).thenReturn(future);

        assertDoesNotThrow(() -> kafkaProducerService.send("reward-events", Map.of("event", "FAILED")));
    }
}
