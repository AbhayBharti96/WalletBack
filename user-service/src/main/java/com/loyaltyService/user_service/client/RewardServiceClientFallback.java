package com.loyaltyService.user_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RewardServiceClientFallback implements RewardServiceClient {
    @Override
    public void createAccount(Long userId) {
        log.error("reward-service unavailable - reward account NOT created for userId={}", userId);
    }
}
