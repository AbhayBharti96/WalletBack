package com.loyaltyService.wallet_service.service.impl;

import com.loyaltyService.wallet_service.entity.LedgerEntry;
import com.loyaltyService.wallet_service.repository.LedgerEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceImplTest {

    @Mock
    private LedgerEntryRepository ledgerRepo;

    @InjectMocks
    private LedgerServiceImpl ledgerService;

    @Test
    void testRecord() {
        when(ledgerRepo.save(any(LedgerEntry.class))).thenAnswer(i -> {
            LedgerEntry entry = i.getArgument(0);
            entry.setId(1L);
            return entry;
        });

        LedgerEntry entry = ledgerService.record(100L, BigDecimal.TEN, LedgerEntry.EntryType.CREDIT, "REF1", "Test");
        
        assertNotNull(entry);
        assertNotNull(entry.getId());
    }
}
