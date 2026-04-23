package com.loyaltyService.reward_service.client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
@Slf4j
@Component
public class WalletClientFallback implements WalletClient {

    @Override
    public ResponseEntity<Void> credit(Long userId, BigDecimal amount) {
        log.warn("Wallet service unavailable — cashback NOT credited userId={}, amount={}", userId, amount);
        // Return a non-2xx so RewardService.redeemPoints() detects the failure
        return ResponseEntity.status(503).build();
    }
}
