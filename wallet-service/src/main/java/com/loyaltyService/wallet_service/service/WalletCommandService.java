package com.loyaltyService.wallet_service.service;

import com.loyaltyService.wallet_service.entity.WalletAccount;

import java.math.BigDecimal;

/**
 * CQRS — Command side: all write operations for Wallet.
 */
public interface WalletCommandService {

    void createWallet(Long userId);

    void recordPendingTopup(Long userId, BigDecimal amount, String idempotencyKey);

    void topup(Long userId, BigDecimal amount, String idempotencyKey);

    void transfer(Long senderId, Long receiverId, BigDecimal amount,
            String idempotencyKey, String description);

    void withdraw(Long userId, BigDecimal amount);

    void creditInternal(Long userId, BigDecimal amount, String source);

    void creditInternal(Long userId, BigDecimal amount);

    void updateStatus(Long userId, WalletAccount.WalletStatus status);
}
