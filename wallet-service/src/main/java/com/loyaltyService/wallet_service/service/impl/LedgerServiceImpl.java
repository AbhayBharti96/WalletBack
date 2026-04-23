package com.loyaltyService.wallet_service.service.impl;


import com.loyaltyService.wallet_service.entity.LedgerEntry;
import com.loyaltyService.wallet_service.repository.LedgerEntryRepository;
import com.loyaltyService.wallet_service.service.LedgerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
@Slf4j @Service @RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {
    private final LedgerEntryRepository ledgerRepo;
    /**
     * Records a ledger entry within the SAME transaction as the balance update.
     * Uses Propagation.MANDATORY to ensure it is never called outside a transaction.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public LedgerEntry record(Long userId, BigDecimal amount, LedgerEntry.EntryType type, String referenceId, String description) {
        LedgerEntry entry = LedgerEntry.builder()
            .userId(userId).amount(amount).type(type)
            .referenceId(referenceId).description(description).build();
        LedgerEntry saved = ledgerRepo.save(entry);
        log.debug("Ledger recorded: userId={}, type={}, amount={}, ref={}", userId, type, amount, referenceId);
        return saved;
    }
}
