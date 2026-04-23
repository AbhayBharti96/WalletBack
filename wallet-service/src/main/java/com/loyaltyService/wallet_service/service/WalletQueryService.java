package com.loyaltyService.wallet_service.service;

import com.loyaltyService.wallet_service.entity.Transaction;
import com.loyaltyService.wallet_service.entity.WalletAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CQRS — Query side: all read operations for Wallet.
 * Results for balance are cached in Redis.
 */
public interface WalletQueryService {

    com.loyaltyService.wallet_service.dto.WalletBalanceResponse getBalance(Long userId);

    Page<Transaction> getTransactions(Long userId, Pageable pageable);

    List<Transaction> getStatement(Long userId, LocalDateTime from, LocalDateTime to);
}
