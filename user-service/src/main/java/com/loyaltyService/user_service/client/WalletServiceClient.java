package com.loyaltyService.user_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "wallet-service", fallback = WalletServiceClientFallback.class)
public interface WalletServiceClient {

    @PostMapping("/api/wallet/internal/create")
    void createWallet(@RequestParam("userId") Long userId);
}
