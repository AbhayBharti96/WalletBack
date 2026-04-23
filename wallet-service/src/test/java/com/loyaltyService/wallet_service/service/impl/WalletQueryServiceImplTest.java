package com.loyaltyService.wallet_service.service.impl;

import com.loyaltyService.wallet_service.dto.WalletBalanceResponse;
import com.loyaltyService.wallet_service.entity.Transaction;
import com.loyaltyService.wallet_service.entity.WalletAccount;
import com.loyaltyService.wallet_service.exception.WalletException;
import com.loyaltyService.wallet_service.mapper.WalletMapper;
import com.loyaltyService.wallet_service.repository.TransactionRepository;
import com.loyaltyService.wallet_service.repository.WalletAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletQueryServiceImplTest {

    @Mock
    private WalletAccountRepository accountRepo;

    @Mock
    private TransactionRepository txnRepo;

    @Mock
    private WalletMapper walletMapper;

    @InjectMocks
    private WalletQueryServiceImpl walletQueryService;

    private WalletAccount testWallet;

    @BeforeEach
    void setUp() {
        testWallet = WalletAccount.builder()
                .id(1L)
                .userId(100L)
                .balance(new BigDecimal("500.00"))
                .status(WalletAccount.WalletStatus.ACTIVE)
                .build();
    }

    @Test
    void testGetBalance_Success() {
        WalletBalanceResponse mapped = WalletBalanceResponse.builder()
                .userId(100L)
                .balance(new BigDecimal("500.00"))
                .status("ACTIVE")
                .build();
        when(accountRepo.findByUserId(100L)).thenReturn(Optional.of(testWallet));
        when(walletMapper.toResponse(testWallet, 100L)).thenReturn(mapped);

        WalletBalanceResponse res = walletQueryService.getBalance(100L);

        assertNotNull(res);
        assertEquals(new BigDecimal("500.00"), res.getBalance());
        assertEquals("ACTIVE", res.getStatus());
    }

    @Test
    void testGetBalance_NotFound() {
        when(accountRepo.findByUserId(999L)).thenReturn(Optional.empty());

        assertThrows(WalletException.class, () -> walletQueryService.getBalance(999L));
    }

    @Test
    void testGetTransactions() {
        Pageable pageable = PageRequest.of(0, 10);
        when(txnRepo.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(100L, 100L, pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        Page<Transaction> page = walletQueryService.getTransactions(100L, pageable);

        assertNotNull(page);
        assertTrue(page.isEmpty());
    }
}
