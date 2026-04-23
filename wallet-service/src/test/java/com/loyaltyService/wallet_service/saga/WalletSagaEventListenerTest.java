package com.loyaltyService.wallet_service.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyService.wallet_service.service.WalletCommandService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletSagaEventListenerTest {

    @Mock
    private WalletCommandService walletCommandService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WalletSagaEventListener listener;

    @Test
    void testOnRewardEvent_PointsRedeemed() throws Exception {
        Map<String, Object> event = new HashMap<>();
        event.put("event", "POINTS_REDEEMED");
        event.put("userId", 100);

        when(objectMapper.readValue("dummyPayload", Map.class)).thenReturn(event);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("reward-events", 0, 0, "key", "dummyPayload");
        listener.onRewardEvent(record);

        // Action is purely logging acknowledgment
        verifyNoInteractions(walletCommandService);
    }

    @Test
    void testOnWalletEvent_TransferCompensation() throws Exception {
        Map<String, Object> event = new HashMap<>();
        event.put("event", "TRANSFER_COMPENSATION");
        event.put("senderId", 200);
        event.put("amount", "50.00");

        when(objectMapper.readValue("dummyPayload", Map.class)).thenReturn(event);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("wallet-events", 0, 0, "key", "dummyPayload");
        listener.onWalletEvent(record);

        verify(walletCommandService, times(1)).creditInternal(200L, new BigDecimal("50.00"), "CASHBACK");
    }
}
