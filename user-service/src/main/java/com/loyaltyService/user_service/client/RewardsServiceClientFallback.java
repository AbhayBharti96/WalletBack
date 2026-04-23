package com.loyaltyService.user_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RewardsServiceClientFallback implements RewardsServiceClient {

    @Override
    public void createRewardAccount(Long userId) {
        log.error("rewards-service unavailable — reward account NOT created for userId={}", userId);
    }
}
