package com.loyaltyService.user_service.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyService.user_service.client.RewardsServiceClient;
import com.loyaltyService.user_service.client.WalletServiceClient;
import com.loyaltyService.user_service.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SAGA — KYC Approval Saga Orchestrator (Choreography-based).
 *
 * Flow:
 * 1. user-service approves KYC → publishes KYC_APPROVED to "kyc-events"
 * 2. THIS listener reacts → creates wallet + reward account
 * 3. On any failure → publishes KYC_SAGA_FAILED as compensating event
 *
 * This removes the synchronous Feign calls from KycServiceImpl.doApprove(),
 * making the KYC approval non-blocking and fault-tolerant.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KycSagaEventListener {

    private static final String EVENT_KEY = "event";
    private static final String USER_ID_KEY = "userId";
    private static final String KYC_APPROVED = "KYC_APPROVED";
    private static final String KYC_SAGA_FAILED = "KYC_SAGA_FAILED";
    private static final String KYC_SAGA_COMPLETED = "KYC_SAGA_COMPLETED";

    private final WalletServiceClient walletServiceClient;
    private final RewardsServiceClient rewardsServiceClient;
    private final KafkaProducerService kafkaProducer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "kyc-events", groupId = "user-service-saga-group", containerFactory = "kafkaListenerContainerFactory")
    public void onKycEvent(ConsumerRecord<String, String> record) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
            String eventType = (String) event.get(EVENT_KEY);

            if (!KYC_APPROVED.equals(eventType))
                return;

            Long userId = Long.valueOf(event.get(USER_ID_KEY).toString());
            log.info("[KYC-SAGA] Received {} for userId={}", KYC_APPROVED, userId);

            boolean walletCreated = false;
            boolean rewardCreated = false;

            // Step 1: create wallet
            try {
                walletServiceClient.createWallet(userId);
                walletCreated = true;
                log.info("[KYC-SAGA] Wallet created for userId={}", userId);
            } catch (Exception e) {
                log.error("[KYC-SAGA] Failed to create wallet for userId={}: {}", userId, e.getMessage());
            }

            // Step 2: create reward account
            try {
                rewardsServiceClient.createRewardAccount(userId);
                rewardCreated = true;
                log.info("[KYC-SAGA] Reward account created for userId={}", userId);
            } catch (Exception e) {
                log.error("[KYC-SAGA] Failed to create reward account for userId={}: {}", userId, e.getMessage());
            }

            if (!walletCreated || !rewardCreated) {
                // Compensating event — downstream services can react
                kafkaProducer.send("kyc-events", Map.of(
                        EVENT_KEY, KYC_SAGA_FAILED,
                        USER_ID_KEY, userId,
                        "walletCreated", walletCreated,
                        "rewardCreated", rewardCreated));
                log.warn("[KYC-SAGA] {} published for userId={}", KYC_SAGA_FAILED, userId);
            } else {
                kafkaProducer.send("kyc-events", Map.of(
                        EVENT_KEY, KYC_SAGA_COMPLETED,
                        USER_ID_KEY, userId));
                log.info("[KYC-SAGA] KYC saga completed successfully for userId={}", userId);
            }

        } catch (Exception e) {
            log.error("[KYC-SAGA] Error processing kyc-events message: {}", e.getMessage(), e);
        }
    }
}
