package com.loyaltyService.wallet_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
@Slf4j @Component
public class RewardClientFallback implements RewardClient {
    @Override
    public void earnPoints(Long userId, BigDecimal amount) {
        log.warn("Rewards service unavailable — points not credited userId={}, amount={}", userId, amount);
    }
}
