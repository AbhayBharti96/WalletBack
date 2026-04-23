package com.loyaltyService.user_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "reward-service", fallback = RewardServiceClientFallback.class)
public interface RewardServiceClient {

    @PostMapping("/api/rewards/internal/create-account")
    void createAccount(@RequestParam("userId") Long userId);
}
