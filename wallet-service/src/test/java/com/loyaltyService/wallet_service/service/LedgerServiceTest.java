package com.loyaltyService.wallet_service.service;

import com.loyaltyService.wallet_service.entity.LedgerEntry;
import com.loyaltyService.wallet_service.repository.LedgerEntryRepository;
import com.loyaltyService.wallet_service.service.impl.LedgerServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerEntryRepository ledgerRepo;

    @InjectMocks
    private LedgerServiceImpl ledgerService;

    @Test
    void recordBuildsAndSavesLedgerEntry() {
        LedgerEntry saved = LedgerEntry.builder()
                .id(1L)
                .userId(1L)
                .amount(new BigDecimal("50.00"))
                .type(LedgerEntry.EntryType.CREDIT)
                .referenceId("REF_1")
                .description("desc")
                .build();
        when(ledgerRepo.save(any(LedgerEntry.class))).thenReturn(saved);

        LedgerEntry result = ledgerService.record(1L, new BigDecimal("50.00"), LedgerEntry.EntryType.CREDIT, "REF_1", "desc");

        assertEquals(1L, result.getId());
        verify(ledgerRepo).save(any(LedgerEntry.class));
    }
}
