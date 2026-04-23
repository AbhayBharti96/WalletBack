package com.loyaltyService.user_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WalletServiceClientFallback implements WalletServiceClient {
    @Override
    public void createWallet(Long userId) {
        log.error("wallet-service unavailable — wallet NOT created for userId={}", userId);
    }
}
