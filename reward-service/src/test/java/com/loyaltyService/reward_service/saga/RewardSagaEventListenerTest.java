package com.loyaltyService.reward_service.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyService.reward_service.service.RewardCommandService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardSagaEventListenerTest {

    @Mock
    private RewardCommandService rewardCommandService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RewardSagaEventListener listener;

    @Test
    void testOnWalletEvent_TopupSuccess() throws Exception {
        Map<String, Object> event = new HashMap<>();
        event.put("event", "TOPUP_SUCCESS");
        event.put("userId", 100);
        event.put("amount", "5000.00");

        when(objectMapper.readValue("dummyPayload", Map.class)).thenReturn(event);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("wallet-events", 0, 0, "key", "dummyPayload");
        listener.onWalletEvent(record);

        verify(rewardCommandService, times(1)).earnPoints(100L, new BigDecimal("5000.00"));
    }

    @Test
    void testOnWalletEvent_Ignored() throws Exception {
        Map<String, Object> event = new HashMap<>();
        event.put("event", "SOME_OTHER_EVENT");

        when(objectMapper.readValue("dummyPayload", Map.class)).thenReturn(event);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("wallet-events", 0, 0, "key", "dummyPayload");
        listener.onWalletEvent(record);

        verifyNoInteractions(rewardCommandService);
    }
}
