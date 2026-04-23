package com.loyaltyService.wallet_service.service.impl;

import com.loyaltyService.wallet_service.client.RewardClient;
import com.loyaltyService.wallet_service.entity.Transaction;
import com.loyaltyService.wallet_service.entity.WalletAccount;
import com.loyaltyService.wallet_service.exception.WalletException;
import com.loyaltyService.wallet_service.repository.TransactionRepository;
import com.loyaltyService.wallet_service.repository.WalletAccountRepository;
import com.loyaltyService.wallet_service.service.KafkaProducerService;
import com.loyaltyService.wallet_service.service.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletCommandServiceImplTest {

    @Mock
    private WalletAccountRepository accountRepo;
    @Mock
    private TransactionRepository txnRepo;
    @Mock
    private LedgerService ledgerService;
    @Mock
    private RewardClient rewardClient;
    @Mock
    private KafkaProducerService kafkaProducer;
    @InjectMocks
    private WalletCommandServiceImpl walletCommandService;

    private WalletAccount testWallet;

    @BeforeEach
    void setUp() {
        testWallet = WalletAccount.builder()
                .id(1L)
                .userId(100L)
                .balance(new BigDecimal("1000.00"))
                .status(WalletAccount.WalletStatus.ACTIVE)
                .build();

        ReflectionTestUtils.setField(walletCommandService, "dailyTopupLimit", new BigDecimal("50000"));
        ReflectionTestUtils.setField(walletCommandService, "dailyTransferLimit", new BigDecimal("25000"));
        ReflectionTestUtils.setField(walletCommandService, "maxDailyTransfers", 10);
    }

    @Test
    void testCreateWallet_Success() {
        when(accountRepo.existsByUserId(100L)).thenReturn(false);
        walletCommandService.createWallet(100L);
        verify(accountRepo, times(1)).save(any(WalletAccount.class));
    }

    @Test
    void testCreateWallet_Conflict() {
        when(accountRepo.existsByUserId(100L)).thenReturn(true);
        assertThrows(WalletException.class, () -> walletCommandService.createWallet(100L));
    }

    @Test
    void testTopup_Success() {
        when(accountRepo.findByUserId(100L)).thenReturn(Optional.of(testWallet));
        when(txnRepo.findByIdempotencyKey("ORD123")).thenReturn(Optional.empty());
        when(txnRepo.sumTodayTopups(eq(100L), eq(Transaction.TxnType.TOPUP), eq(Transaction.TxnStatus.SUCCESS), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        walletCommandService.topup(100L, new BigDecimal("500"), "ORD123");

        assertEquals(new BigDecimal("1500.00"), testWallet.getBalance());
        verify(txnRepo, times(1)).save(any());
        verify(kafkaProducer, times(1)).send(eq("wallet-events"), anyMap());
    }

    @Test
    void testWithdraw_InsufficientBalance() {
        when(accountRepo.findByUserId(100L)).thenReturn(Optional.of(testWallet));

        assertThrows(WalletException.class, () -> 
            walletCommandService.withdraw(100L, new BigDecimal("1500.00")));
    }
}
