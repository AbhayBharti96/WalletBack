package com.loyaltyService.user_service.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyService.user_service.client.RewardsServiceClient;
import com.loyaltyService.user_service.client.WalletServiceClient;
import com.loyaltyService.user_service.service.KafkaProducerService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycSagaEventListenerTest {

    @Mock
    private WalletServiceClient walletServiceClient;

    @Mock
    private RewardsServiceClient rewardsServiceClient;

    @Mock
    private KafkaProducerService kafkaProducer;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private KycSagaEventListener listener;

    @Test
    void testOnKycEvent_Approved_Success() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("event", "KYC_APPROVED");
        map.put("userId", 1L);

        when(objectMapper.readValue("dummyPayload", Map.class)).thenReturn(map);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("kyc-events", 0, 0, "key", "dummyPayload");
        listener.onKycEvent(record);

        verify(walletServiceClient, times(1)).createWallet(1L);
        verify(rewardsServiceClient, times(1)).createRewardAccount(1L);
        verify(kafkaProducer, times(1)).send(eq("kyc-events"), any(Map.class));
    }

    @Test
    void testOnKycEvent_Approved_Failure() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("event", "KYC_APPROVED");
        map.put("userId", 1L);

        when(objectMapper.readValue("dummyPayload", Map.class)).thenReturn(map);
        doThrow(new RuntimeException("API Error")).when(walletServiceClient).createWallet(1L);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("kyc-events", 0, 0, "key", "dummyPayload");
        listener.onKycEvent(record);

        verify(walletServiceClient, times(1)).createWallet(1L);
        verify(rewardsServiceClient, times(1)).createRewardAccount(1L); // Still runs step 2
        verify(kafkaProducer, times(1)).send(eq("kyc-events"),
                argThat(argument -> "KYC_SAGA_FAILED".equals(((Map<String, Object>) argument).get("event"))));
    }

    @Test
    void testOnKycEvent_NotApproved() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("event", "KYC_SUBMITTED");
        map.put("userId", 1L);

        when(objectMapper.readValue("dummyPayload", Map.class)).thenReturn(map);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("kyc-events", 0, 0, "key", "dummyPayload");
        listener.onKycEvent(record);

        verifyNoInteractions(walletServiceClient, rewardsServiceClient, kafkaProducer);
    }
}
