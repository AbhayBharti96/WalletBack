package com.loyaltyService.reward_service.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyService.reward_service.service.RewardCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * SAGA — Reward-service participant in the Wallet Topup Saga.
 *
 * Flow:
 * 1. wallet-service tops up → publishes TOPUP_SUCCESS to "wallet-events"
 * 2. THIS listener reacts → calls earnPoints (saga step)
 * 3. Any failure is logged; no compensating action needed for point earning
 * (eventual consistency: points may be slightly delayed, never double-credited
 * because wallet topup already uses idempotency keys)
 *
 * This replaces the synchronous Feign call from WalletServiceImpl →
 * rewardClient.earnPoints()
 * for the topup case, making it fully async/event-driven.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RewardSagaEventListener {

    private final RewardCommandService rewardCommandService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "wallet-events", groupId = "reward-service-saga-group", containerFactory = "kafkaListenerContainerFactory")
    public void onWalletEvent(ConsumerRecord<String, String> record) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
            String eventType = (String) event.get("event");

            if ("TOPUP_SUCCESS".equals(eventType)) {
                handleTopupSuccess(event);
            } else if ("POINTS_REDEEM_FAILED".equals(eventType)) {
                handleRedeemFailedCompensation(event);
            }
        } catch (Exception e) {
            log.error("[REWARD-SAGA] Error processing wallet-events: {}", e.getMessage(), e);
        }
    }

    // ── Step: earn points after topup ─────────────────────────────────────────

    private void handleTopupSuccess(Map<String, Object> event) {
        Long userId = Long.valueOf(event.get("userId").toString());
        BigDecimal amount = new BigDecimal(event.get("amount").toString());

        log.info("[REWARD-SAGA] TOPUP_SUCCESS received — earning points for userId={}, amount={}", userId, amount);
        try {
            rewardCommandService.earnPoints(userId, amount);
            log.info("[REWARD-SAGA] Points earned successfully for userId={}", userId);
        } catch (Exception e) {
            log.error("[REWARD-SAGA] Failed to earn points for userId={}: {}", userId, e.getMessage(), e);
            // No compensation needed — topup already committed; points are best-effort
        }
    }

    // ── Compensation: log that points redeem failed (wallet already compensated) ─

    private void handleRedeemFailedCompensation(Map<String, Object> event) {
        Long userId = Long.valueOf(event.get("userId").toString());
        Object points = event.get("points");
        log.warn(
                "[REWARD-SAGA] POINTS_REDEEM_FAILED received — compensation already applied in RewardCommandServiceImpl. userId={}, points={}",
                userId, points);
    }
}
