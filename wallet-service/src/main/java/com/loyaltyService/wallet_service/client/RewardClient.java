package com.loyaltyService.wallet_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
@FeignClient(name = "rewards-service", fallback = RewardClientFallback.class)
public interface RewardClient {
    @PostMapping("/api/rewards/internal/earn")
    void earnPoints(@RequestParam("userId") Long userId, @RequestParam("amount") BigDecimal amount);
}
