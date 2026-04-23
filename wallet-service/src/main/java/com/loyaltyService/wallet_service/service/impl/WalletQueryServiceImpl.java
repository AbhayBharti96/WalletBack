package com.loyaltyService.wallet_service.service.impl;

import com.loyaltyService.wallet_service.dto.WalletBalanceResponse;
import com.loyaltyService.wallet_service.entity.Transaction;
import com.loyaltyService.wallet_service.mapper.WalletMapper;
import com.loyaltyService.wallet_service.entity.WalletAccount;
import com.loyaltyService.wallet_service.exception.WalletException;
import com.loyaltyService.wallet_service.repository.TransactionRepository;
import com.loyaltyService.wallet_service.repository.WalletAccountRepository;
import com.loyaltyService.wallet_service.service.WalletQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CQRS — Query implementation for Wallet.
 * Reads are separated from writes and the balance is cached in Redis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletQueryServiceImpl implements WalletQueryService {

    private final WalletAccountRepository accountRepo;
    private final TransactionRepository txnRepo;
    private final WalletMapper walletMapper;

    @Override
    @Cacheable(value = "wallet-balance", key = "#userId")
    public WalletBalanceResponse getBalance(Long userId) {
        log.debug("Cache miss — loading wallet balance from DB for userId={}", userId);
        WalletAccount acc = findWallet(userId);
        return walletMapper.toResponse(acc, userId);
    }

    @Override
    public Page<Transaction> getTransactions(Long userId, Pageable pageable) {
        return txnRepo.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(userId, userId, pageable);
    }

    @Override
    public List<Transaction> getStatement(Long userId, LocalDateTime from, LocalDateTime to) {
        return txnRepo.findStatement(userId, from, to);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private WalletAccount findWallet(Long userId) {
        return accountRepo.findByUserId(userId)
                .orElseThrow(() -> new WalletException("Wallet not found for userId: " + userId, HttpStatus.NOT_FOUND));
    }
}
