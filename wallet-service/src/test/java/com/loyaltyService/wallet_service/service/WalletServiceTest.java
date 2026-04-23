package com.loyaltyService.wallet_service.service;

import com.loyaltyService.wallet_service.client.RewardClient;
import com.loyaltyService.wallet_service.dto.WalletBalanceResponse;
import com.loyaltyService.wallet_service.entity.LedgerEntry;
import com.loyaltyService.wallet_service.entity.Transaction;
import com.loyaltyService.wallet_service.entity.WalletAccount;
import com.loyaltyService.wallet_service.exception.WalletException;
import com.loyaltyService.wallet_service.mapper.WalletMapper;
import com.loyaltyService.wallet_service.repository.TransactionRepository;
import com.loyaltyService.wallet_service.repository.WalletAccountRepository;
import com.loyaltyService.wallet_service.service.KafkaProducerService;
import com.loyaltyService.wallet_service.service.LedgerService;
import com.loyaltyService.wallet_service.service.impl.WalletCommandServiceImpl;
import com.loyaltyService.wallet_service.service.impl.WalletQueryServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock private WalletAccountRepository accountRepo;
    @Mock private TransactionRepository txnRepo;
    @Mock private LedgerService ledgerService;
    @Mock private RewardClient rewardClient;
    @Mock private KafkaProducerService kafkaProducer;
    @Mock private WalletMapper walletMapper;

    @InjectMocks
    private WalletCommandServiceImpl walletCommandService;

    @InjectMocks
    private WalletQueryServiceImpl walletQueryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(walletCommandService, "dailyTopupLimit", new BigDecimal("50000"));
        ReflectionTestUtils.setField(walletCommandService, "dailyTransferLimit", new BigDecimal("25000"));
        ReflectionTestUtils.setField(walletCommandService, "maxDailyTransfers", 10);
    }

    @Test
    void createWalletSavesNewWallet() {
        when(accountRepo.existsByUserId(1L)).thenReturn(false);

        walletCommandService.createWallet(1L);

        verify(accountRepo).save(any(WalletAccount.class));
    }

    @Test
    void createWalletThrowsWhenAlreadyExists() {
        when(accountRepo.existsByUserId(1L)).thenReturn(true);

        WalletException exception = assertThrows(WalletException.class, () -> walletCommandService.createWallet(1L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void getBalanceReturnsMappedResponse() {
        WalletAccount account = activeWallet(1L, "100.00");
        WalletBalanceResponse mapped = WalletBalanceResponse.builder()
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .status("ACTIVE")
                .build();
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        when(walletMapper.toResponse(account, 1L)).thenReturn(mapped);

        WalletBalanceResponse response = walletQueryService.getBalance(1L);

        assertEquals(1L, response.getUserId());
        assertEquals(new BigDecimal("100.00"), response.getBalance());
        assertEquals("ACTIVE", response.getStatus());
    }

    @Test
    void topupIgnoresDuplicateIdempotencyKey() {
        when(txnRepo.findByIdempotencyKey("idem")).thenReturn(Optional.of(Transaction.builder().id(1L).build()));

        walletCommandService.topup(1L, new BigDecimal("100"), "idem");

        verify(ledgerService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void topupThrowsWhenDailyLimitExceeded() {
        WalletAccount account = activeWallet(1L, "100.00");
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        when(txnRepo.sumTodayTopups(eq(1L), eq(Transaction.TxnType.TOPUP), eq(Transaction.TxnStatus.SUCCESS), any(), any()))
                .thenReturn(new BigDecimal("49990"));

        WalletException exception = assertThrows(WalletException.class,
                () -> walletCommandService.topup(1L, new BigDecimal("20"), "idem-2"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void topupCreditsWalletSavesTransactionAndPublishesEvent() {
        WalletAccount account = activeWallet(1L, "100.00");
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        when(txnRepo.findByIdempotencyKey("idem")).thenReturn(Optional.empty());
        when(txnRepo.sumTodayTopups(eq(1L), eq(Transaction.TxnType.TOPUP), eq(Transaction.TxnStatus.SUCCESS), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        walletCommandService.topup(1L, new BigDecimal("50.00"), "idem");

        assertEquals(new BigDecimal("150.00"), account.getBalance());
        verify(ledgerService).record(eq(1L), eq(new BigDecimal("50.00")), eq(LedgerEntry.EntryType.CREDIT), org.mockito.ArgumentMatchers.startsWith("TOPUP_"), eq("Wallet top-up"));
        verify(accountRepo).save(account);
        verify(kafkaProducer).send(eq("wallet-events"), any(Map.class));
    }

    @Test
    void transferThrowsWhenSenderEqualsReceiver() {
        WalletException exception = assertThrows(WalletException.class,
                () -> walletCommandService.transfer(1L, 1L, new BigDecimal("10"), null, "desc"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void transferIgnoresDuplicateIdempotencyKey() {
        when(txnRepo.findByIdempotencyKey("idem")).thenReturn(Optional.of(Transaction.builder().id(1L).build()));

        walletCommandService.transfer(1L, 2L, new BigDecimal("10"), "idem", "desc");

        verify(accountRepo, never()).save(any(WalletAccount.class));
    }

    @Test
    void transferThrowsWhenSenderHasInsufficientBalance() {
        WalletAccount sender = activeWallet(1L, "10.00");
        WalletAccount receiver = activeWallet(2L, "0.00");
        when(txnRepo.findByIdempotencyKey("idem")).thenReturn(Optional.empty());
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(sender));
        when(accountRepo.findByUserId(2L)).thenReturn(Optional.of(receiver));

        WalletException exception = assertThrows(WalletException.class,
                () -> walletCommandService.transfer(1L, 2L, new BigDecimal("20"), "idem", "desc"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void transferThrowsWhenDailyAmountLimitExceeded() {
        WalletAccount sender = activeWallet(1L, "1000.00");
        WalletAccount receiver = activeWallet(2L, "0.00");
        when(txnRepo.findByIdempotencyKey("idem")).thenReturn(Optional.empty());
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(sender));
        when(accountRepo.findByUserId(2L)).thenReturn(Optional.of(receiver));
        when(txnRepo.sumTodayTransfers(eq(1L), eq(Transaction.TxnType.TRANSFER), eq(Transaction.TxnStatus.SUCCESS), any(), any()))
                .thenReturn(new BigDecimal("24990"));

        WalletException exception = assertThrows(WalletException.class,
                () -> walletCommandService.transfer(1L, 2L, new BigDecimal("20"), "idem", "desc"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void transferThrowsWhenDailyCountLimitReached() {
        WalletAccount sender = activeWallet(1L, "1000.00");
        WalletAccount receiver = activeWallet(2L, "0.00");
        when(txnRepo.findByIdempotencyKey("idem")).thenReturn(Optional.empty());
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(sender));
        when(accountRepo.findByUserId(2L)).thenReturn(Optional.of(receiver));
        when(txnRepo.sumTodayTransfers(eq(1L), eq(Transaction.TxnType.TRANSFER), eq(Transaction.TxnStatus.SUCCESS), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(txnRepo.countTodayTransfers(eq(1L), eq(Transaction.TxnType.TRANSFER), eq(Transaction.TxnStatus.SUCCESS), any(), any()))
                .thenReturn(10L);

        WalletException exception = assertThrows(WalletException.class,
                () -> walletCommandService.transfer(1L, 2L, new BigDecimal("20"), "idem", "desc"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void transferDebitsAndCreditsWallets() {
        WalletAccount sender = activeWallet(1L, "100.00");
        WalletAccount receiver = activeWallet(2L, "40.00");
        when(txnRepo.findByIdempotencyKey("idem")).thenReturn(Optional.empty());
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(sender));
        when(accountRepo.findByUserId(2L)).thenReturn(Optional.of(receiver));
        when(txnRepo.sumTodayTransfers(eq(1L), eq(Transaction.TxnType.TRANSFER), eq(Transaction.TxnStatus.SUCCESS), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(txnRepo.countTodayTransfers(eq(1L), eq(Transaction.TxnType.TRANSFER), eq(Transaction.TxnStatus.SUCCESS), any(), any()))
                .thenReturn(0L);

        walletCommandService.transfer(1L, 2L, new BigDecimal("25.00"), "idem", "gift");

        assertEquals(new BigDecimal("75.00"), sender.getBalance());
        assertEquals(new BigDecimal("65.00"), receiver.getBalance());
        verify(ledgerService).record(eq(1L), eq(new BigDecimal("25.00")), eq(LedgerEntry.EntryType.DEBIT), org.mockito.ArgumentMatchers.startsWith("TXN_"), eq("gift"));
        verify(ledgerService).record(eq(2L), eq(new BigDecimal("25.00")), eq(LedgerEntry.EntryType.CREDIT), org.mockito.ArgumentMatchers.startsWith("TXN_"), eq("gift"));
        verify(rewardClient).earnPoints(1L, new BigDecimal("25.00"));
        verify(kafkaProducer).send(eq("wallet-events"), any(Map.class));
    }

    @Test
    void withdrawThrowsWhenBalanceInsufficient() {
        WalletAccount account = activeWallet(1L, "10.00");
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(account));

        WalletException exception = assertThrows(WalletException.class,
                () -> walletCommandService.withdraw(1L, new BigDecimal("20")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void withdrawDebitsWalletAndStoresTransaction() {
        WalletAccount account = activeWallet(1L, "100.00");
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(account));

        walletCommandService.withdraw(1L, new BigDecimal("25.00"));

        assertEquals(new BigDecimal("75.00"), account.getBalance());
        verify(ledgerService).record(eq(1L), eq(new BigDecimal("25.00")), eq(LedgerEntry.EntryType.DEBIT), org.mockito.ArgumentMatchers.startsWith("WITHDRAW_"), eq("Wallet withdrawal"));
        verify(accountRepo).save(account);
        verify(txnRepo).save(any(Transaction.class));
    }

    @Test
    void creditInternalUsesCashbackTypeByDefault() {
        WalletAccount account = activeWallet(1L, "100.00");
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(account));

        walletCommandService.creditInternal(1L, new BigDecimal("10.00"));

        assertEquals(new BigDecimal("110.00"), account.getBalance());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(txnRepo).save(captor.capture());
        assertEquals(Transaction.TxnType.CASHBACK, captor.getValue().getType());
    }

    @Test
    void creditInternalUsesRedeemTypeWhenSourceIsRedeem() {
        WalletAccount account = activeWallet(1L, "100.00");
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(account));

        walletCommandService.creditInternal(1L, new BigDecimal("10.00"), "REDEEM");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(txnRepo).save(captor.capture());
        assertEquals(Transaction.TxnType.REDEEM, captor.getValue().getType());
        assertEquals("Points redeemed → ₹10.00 credited", captor.getValue().getDescription());
    }

    @Test
    void updateStatusDelegatesToRepository() {
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(activeWallet(1L, "0.00")));

        walletCommandService.updateStatus(1L, WalletAccount.WalletStatus.BLOCKED);

        verify(accountRepo).updateStatus(1L, WalletAccount.WalletStatus.BLOCKED);
    }

    @Test
    void getTransactionsDelegatesToRepository() {
        when(txnRepo.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(1L, 1L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(Transaction.builder().id(1L).build())));

        var page = walletQueryService.getTransactions(1L, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
    }

    @Test
    void getStatementDelegatesToRepository() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        when(txnRepo.findStatement(1L, from, to)).thenReturn(List.of(Transaction.builder().id(1L).build()));

        var statement = walletQueryService.getStatement(1L, from, to);

        assertEquals(1, statement.size());
    }

    @Test
    void getBalanceThrowsWhenWalletMissing() {
        when(accountRepo.findByUserId(99L)).thenReturn(Optional.empty());

        WalletException exception = assertThrows(WalletException.class, () -> walletQueryService.getBalance(99L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void topupThrowsWhenWalletBlocked() {
        WalletAccount account = blockedWallet(1L, "100.00");
        when(txnRepo.findByIdempotencyKey("idem")).thenReturn(Optional.empty());
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(account));

        WalletException exception = assertThrows(WalletException.class,
                () -> walletCommandService.topup(1L, new BigDecimal("10"), "idem"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    private WalletAccount activeWallet(Long userId, String balance) {
        return WalletAccount.builder()
                .id(userId)
                .userId(userId)
                .balance(new BigDecimal(balance))
                .status(WalletAccount.WalletStatus.ACTIVE)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private WalletAccount blockedWallet(Long userId, String balance) {
        return WalletAccount.builder()
                .id(userId)
                .userId(userId)
                .balance(new BigDecimal(balance))
                .status(WalletAccount.WalletStatus.BLOCKED)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
