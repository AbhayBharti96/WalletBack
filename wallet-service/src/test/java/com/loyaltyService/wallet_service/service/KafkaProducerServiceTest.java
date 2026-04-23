package com.loyaltyService.wallet_service.service;

import com.loyaltyService.wallet_service.service.impl.KafkaProducerServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private KafkaProducerServiceImpl kafkaProducerService;

    @Test
    void sendSerializesPayloadAndDelegatesToTemplate() {
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));

        kafkaProducerService.send("wallet-events", Map.of("userId", 1, "amount", 100));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("wallet-events"), payloadCaptor.capture());
        String payload = payloadCaptor.getValue();
        assertTrue(payload.contains("\"userId\":1"));
        assertTrue(payload.contains("\"amount\":100"));
    }

    @Test
    void sendSwallowsCallbackFailure() {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("kafka down"));
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(future);

        assertDoesNotThrow(() -> kafkaProducerService.send("wallet-events", Map.of("event", "FAILED")));
    }

    @Test
    void sendSwallowsSerializationFailure() {
        Object cyclic = new Object() {
            @SuppressWarnings("unused")
            public final Object self = this;
        };

        assertDoesNotThrow(() -> kafkaProducerService.send("wallet-events", cyclic));
    }
}
