package com.loyaltyService.reward_service.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;

@FeignClient(name = "wallet-service", fallback = WalletClientFallback.class)
public interface WalletClient {

    @PostMapping("/api/wallet/internal/credit")
    ResponseEntity<Void> credit(
            @RequestParam("userId") Long userId,
            @RequestParam("amount") BigDecimal amount);
}
