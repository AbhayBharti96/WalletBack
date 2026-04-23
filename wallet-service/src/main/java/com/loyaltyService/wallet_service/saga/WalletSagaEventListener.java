package com.loyaltyService.wallet_service.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyService.wallet_service.service.WalletCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * SAGA — Wallet-service compensating event listener.
 *
 * Flow (Transfer Saga):
 * 1. wallet-service completes transfer → publishes TRANSFER_SUCCESS
 * 2. If a downstream consumer reports failure (e.g., fraud check, notification)
 * a TRANSFER_COMPENSATION event can trigger a reverse credit here
 *
 * Flow (Reward Redeem Saga):
 * - reward-events POINTS_REDEEMED → wallet already credited during the saga
 * step
 * - reward-events REWARD_REDEEM_FAILED → compensation already handled inside
 * RewardCommandServiceImpl
 * (points re-credited before wallet is ever called), so no additional action
 * here
 *
 * This listener primarily handles any external compensation requests targeting
 * wallet-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletSagaEventListener {

    private final WalletCommandService walletCommandService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "reward-events", groupId = "wallet-service-saga-group", containerFactory = "kafkaListenerContainerFactory")
    public void onRewardEvent(ConsumerRecord<String, String> record) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
            String eventType = (String) event.get("event");

            switch (eventType) {
                case "POINTS_REDEEMED" -> handlePointsRedeemed(event);
                case "POINTS_EARNED" ->
                    log.debug("[WALLET-SAGA] POINTS_EARNED acknowledged for userId={}", event.get("userId"));
                default -> log.debug("[WALLET-SAGA] Ignoring reward event: {}", eventType);
            }
        } catch (Exception e) {
            log.error("[WALLET-SAGA] Error processing reward-events message: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "wallet-events", groupId = "wallet-service-compensation-group", containerFactory = "kafkaListenerContainerFactory")
    public void onWalletEvent(ConsumerRecord<String, String> record) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
            String eventType = (String) event.get("event");

            // Listen for saga compensation requests
            if ("TRANSFER_COMPENSATION".equals(eventType)) {
                handleTransferCompensation(event);
            }
        } catch (Exception e) {
            log.error("[WALLET-SAGA] Error processing compensation event: {}", e.getMessage(), e);
        }
    }

    // ── Saga steps ────────────────────────────────────────────────────────────

    /**
     * Acknowledge that points were redeemed and wallet was already credited
     * by RewardCommandServiceImpl via direct Feign call during the saga step.
     */
    private void handlePointsRedeemed(Map<String, Object> event) {
        Long userId = Long.valueOf(event.get("userId").toString());
        log.info("[WALLET-SAGA] POINTS_REDEEMED acknowledged — wallet credit already applied for userId={}", userId);
    }

    /**
     * Compensating transaction for a failed transfer:
     * Reverses the transfer by re-crediting the sender.
     */
    private void handleTransferCompensation(Map<String, Object> event) {
        Long senderId = Long.valueOf(event.get("senderId").toString());
        BigDecimal amount = new BigDecimal(event.get("amount").toString());
        String source = "CASHBACK"; // Mark as reversal cashback

        log.warn("[WALLET-SAGA] Applying TRANSFER_COMPENSATION: crediting senderId={}, amount={}", senderId, amount);
        try {
            walletCommandService.creditInternal(senderId, amount, source);
            log.info("[WALLET-SAGA] Transfer compensation applied for senderId={}", senderId);
        } catch (Exception e) {
            log.error("[WALLET-SAGA] Compensation FAILED for senderId={}: {}", senderId, e.getMessage(), e);
            // Dead-letter or alert here in a production system
        }
    }
}
