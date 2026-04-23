package com.loyaltyService.user_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "rewards-service", fallback = RewardsServiceClientFallback.class)
public interface RewardsServiceClient {

    @PostMapping("/rewards/internal/create-account")
    void createRewardAccount(@RequestParam("userId") Long userId);
}
