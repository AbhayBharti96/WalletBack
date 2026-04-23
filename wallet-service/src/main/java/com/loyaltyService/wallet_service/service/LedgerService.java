package com.loyaltyService.wallet_service.service;

import java.math.BigDecimal;

import com.loyaltyService.wallet_service.entity.LedgerEntry;

public interface LedgerService {
	LedgerEntry record(Long userId, BigDecimal amount, LedgerEntry.EntryType type, String referenceId, String description);
}
