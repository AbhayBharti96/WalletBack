package com.loyaltyService.reward_service.service;

import com.loyaltyService.reward_service.client.WalletClient;
import com.loyaltyService.reward_service.dto.PageResponse;
import com.loyaltyService.reward_service.dto.RewardItemRequest;
import com.loyaltyService.reward_service.dto.RewardSummaryDto;
import com.loyaltyService.reward_service.entity.Redemption;
import com.loyaltyService.reward_service.entity.RewardAccount;
import com.loyaltyService.reward_service.entity.RewardItem;
import com.loyaltyService.reward_service.entity.RewardTransaction;
import com.loyaltyService.reward_service.exception.RewardException;
import com.loyaltyService.reward_service.repository.RedemptionRepository;
import com.loyaltyService.reward_service.repository.RewardItemRepository;
import com.loyaltyService.reward_service.repository.RewardRepository;
import com.loyaltyService.reward_service.repository.RewardTransactionRepository;
import com.loyaltyService.reward_service.service.impl.RewardCommandServiceImpl;
import com.loyaltyService.reward_service.service.impl.RewardQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock private RewardRepository rewardRepo;
    @Mock private RewardTransactionRepository txnRepo;
    @Mock private RewardItemRepository itemRepo;
    @Mock private RedemptionRepository redemptionRepo;
    @Mock private WalletClient walletClient;
    @Mock private KafkaProducerService kafkaProducer;

    @InjectMocks
    private RewardCommandServiceImpl rewardCommandService;

    @InjectMocks
    private RewardQueryServiceImpl rewardQueryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rewardCommandService, "pointsPerRupee", 100);
        ReflectionTestUtils.setField(rewardCommandService, "minRedeemPoints", 100);
        ReflectionTestUtils.setField(rewardCommandService, "maxDailyRedeemPoints", 5000);
        ReflectionTestUtils.setField(rewardCommandService, "goldThreshold", 1000);
        ReflectionTestUtils.setField(rewardCommandService, "platinumThreshold", 5000);
        ReflectionTestUtils.setField(rewardCommandService, "firstTopupBonus", 100);
        ReflectionTestUtils.setField(rewardQueryService, "goldThreshold", 1000);
        ReflectionTestUtils.setField(rewardQueryService, "platinumThreshold", 5000);
    }

    @Test
    void earnPointsCreatesAccountAwardsBonusAndPublishesEvent() {
        RewardAccount created = RewardAccount.builder().userId(1L).points(0).tier(RewardAccount.Tier.SILVER).firstTopupDone(false).build();
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.empty());
        when(rewardRepo.save(any(RewardAccount.class))).thenReturn(created);

        rewardCommandService.earnPoints(1L, new BigDecimal("1000"));

        verify(txnRepo, times(2)).save(any(RewardTransaction.class));
        verify(kafkaProducer).send(eq("reward-events"), any(Map.class));
        verify(rewardRepo, times(2)).save(any(RewardAccount.class));
    }

    @Test
    void earnPointsSkipsEarnTransactionWhenAmountTooSmallButStillGivesFirstBonus() {
        RewardAccount account = RewardAccount.builder().userId(1L).points(0).tier(RewardAccount.Tier.SILVER).firstTopupDone(false).build();
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(account));

        rewardCommandService.earnPoints(1L, new BigDecimal("50"));

        ArgumentCaptor<RewardTransaction> captor = ArgumentCaptor.forClass(RewardTransaction.class);
        verify(txnRepo).save(captor.capture());
        assertEquals(RewardTransaction.TxnType.BONUS, captor.getValue().getType());
        assertEquals(100, account.getPoints());
    }

    @Test
    void earnPointsUpdatesTierToGold() {
        RewardAccount account = RewardAccount.builder().userId(1L).points(950).tier(RewardAccount.Tier.SILVER).firstTopupDone(true).build();
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(account));

        rewardCommandService.earnPoints(1L, new BigDecimal("10000"));

        assertEquals(RewardAccount.Tier.GOLD, account.getTier());
    }

    @Test
    void earnPointsInitializesLegacyNullFieldsBeforeApplyingRewards() {
        RewardAccount legacyAccount = RewardAccount.builder()
                .userId(1L)
                .points(null)
                .tier(null)
                .firstTopupDone(null)
                .build();
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(legacyAccount));

        rewardCommandService.earnPoints(1L, new BigDecimal("500"));

        assertEquals(105, legacyAccount.getPoints());
        assertEquals(RewardAccount.Tier.SILVER, legacyAccount.getTier());
        assertEquals(Boolean.TRUE, legacyAccount.getFirstTopupDone());
        verify(txnRepo, times(2)).save(any(RewardTransaction.class));
    }

    @Test
    void addCatalogItemSavesMappedItem() {
        RewardItemRequest request = new RewardItemRequest(
                "Coupon",
                "Desc",
                500,
                "COUPON",
                10,
                "GOLD",
                BigDecimal.ZERO
        );
        when(itemRepo.save(any(RewardItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RewardItem item = rewardCommandService.addCatalogItem(request);

        assertEquals("Coupon", item.getName());
        assertEquals(RewardItem.ItemType.COUPON, item.getType());
        assertEquals(true, item.getActive());
    }

    @Test
    void updateCatalogItemUpdatesExistingItem() {
        RewardItem existing = RewardItem.builder()
                .id(5L)
                .name("Old")
                .description("Old desc")
                .pointsRequired(300)
                .type(RewardItem.ItemType.COUPON)
                .stock(5)
                .tierRequired("SILVER")
                .active(true)
                .build();
        RewardItemRequest request = new RewardItemRequest(
                "New",
                "New desc",
                450,
                "VOUCHER",
                9,
                "GOLD",
                BigDecimal.ZERO
        );
        when(itemRepo.findById(5L)).thenReturn(Optional.of(existing));
        when(itemRepo.save(any(RewardItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RewardItem updated = rewardCommandService.updateCatalogItem(5L, request);

        assertEquals("New", updated.getName());
        assertEquals(450, updated.getPointsRequired());
        assertEquals(RewardItem.ItemType.VOUCHER, updated.getType());
        assertEquals(9, updated.getStock());
    }

    @Test
    void deleteCatalogItemDeletesExistingItem() {
        RewardItem existing = RewardItem.builder().id(6L).name("ToDelete").build();
        when(itemRepo.findById(6L)).thenReturn(Optional.of(existing));

        rewardCommandService.deleteCatalogItem(6L);

        verify(itemRepo).delete(existing);
    }

    @Test
    void redeemPointsThrowsWhenBelowMinimum() {
        RewardException exception = assertThrows(RewardException.class, () -> rewardCommandService.redeemPoints(1L, 50));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void redeemPointsThrowsWhenDailyLimitExceeded() {
        when(txnRepo.sumRedeemedPointsToday(eq(1L), eq(RewardTransaction.TxnType.REDEEM), any(), any())).thenReturn(4900);

        RewardException exception = assertThrows(RewardException.class, () -> rewardCommandService.redeemPoints(1L, 200));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void redeemPointsThrowsWhenAccountMissing() {
        when(txnRepo.sumRedeemedPointsToday(eq(1L), eq(RewardTransaction.TxnType.REDEEM), any(), any())).thenReturn(0);
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.empty());

        RewardException exception = assertThrows(RewardException.class, () -> rewardCommandService.redeemPoints(1L, 200));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void redeemPointsThrowsWhenInsufficientPoints() {
        RewardAccount account = RewardAccount.builder().userId(1L).points(150).tier(RewardAccount.Tier.SILVER).build();
        when(txnRepo.sumRedeemedPointsToday(eq(1L), eq(RewardTransaction.TxnType.REDEEM), any(), any())).thenReturn(0);
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(account));

        RewardException exception = assertThrows(RewardException.class, () -> rewardCommandService.redeemPoints(1L, 200));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void redeemPointsCreditsWalletAndSavesTransaction() {
        RewardAccount account = RewardAccount.builder().userId(1L).points(500).tier(RewardAccount.Tier.SILVER).build();
        when(txnRepo.sumRedeemedPointsToday(eq(1L), eq(RewardTransaction.TxnType.REDEEM), any(), any())).thenReturn(0);
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        when(walletClient.credit(1L, new BigDecimal("2"))).thenReturn(ResponseEntity.ok().build());

        rewardCommandService.redeemPoints(1L, 200);

        assertEquals(300, account.getPoints());
        verify(walletClient).credit(1L, new BigDecimal("2"));
        verify(txnRepo).save(any(RewardTransaction.class));
    }

    @Test
    void convertPointsToCashDelegatesToRedeemLogic() {
        RewardAccount account = RewardAccount.builder().userId(1L).points(500).tier(RewardAccount.Tier.SILVER).build();
        when(txnRepo.sumRedeemedPointsToday(eq(1L), eq(RewardTransaction.TxnType.REDEEM), any(), any())).thenReturn(0);
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        when(walletClient.credit(1L, new BigDecimal("2"))).thenReturn(ResponseEntity.ok().build());

        rewardCommandService.convertPointsToCash(1L, 200);

        verify(walletClient).credit(1L, new BigDecimal("2"));
    }

    @Test
    void redeemRewardThrowsWhenItemMissing() {
        RewardAccount account = RewardAccount.builder().userId(1L).points(1000).tier(RewardAccount.Tier.GOLD).build();
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        when(itemRepo.findById(1L)).thenReturn(Optional.empty());

        RewardException exception = assertThrows(RewardException.class, () -> rewardCommandService.redeemReward(1L, 1L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void redeemRewardThrowsWhenItemInactive() {
        RewardAccount account = RewardAccount.builder().userId(1L).points(1000).tier(RewardAccount.Tier.GOLD).build();
        RewardItem item = RewardItem.builder().id(1L).active(false).stock(1).pointsRequired(100).type(RewardItem.ItemType.COUPON).build();
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        when(itemRepo.findById(1L)).thenReturn(Optional.of(item));

        assertThrows(RewardException.class, () -> rewardCommandService.redeemReward(1L, 1L));
    }

    @Test
    void redeemRewardThrowsWhenOutOfStock() {
        RewardAccount account = RewardAccount.builder().userId(1L).points(1000).tier(RewardAccount.Tier.GOLD).build();
        RewardItem item = RewardItem.builder().id(1L).active(true).stock(0).pointsRequired(100).type(RewardItem.ItemType.COUPON).build();
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        when(itemRepo.findById(1L)).thenReturn(Optional.of(item));

        assertThrows(RewardException.class, () -> rewardCommandService.redeemReward(1L, 1L));
    }

    @Test
    void redeemRewardThrowsWhenTierIneligible() {
        RewardAccount account = RewardAccount.builder().userId(1L).points(1000).tier(RewardAccount.Tier.SILVER).build();
        RewardItem item = RewardItem.builder().id(1L).active(true).stock(1).pointsRequired(100).type(RewardItem.ItemType.COUPON).tierRequired("GOLD").build();
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        when(itemRepo.findById(1L)).thenReturn(Optional.of(item));

        assertThrows(RewardException.class, () -> rewardCommandService.redeemReward(1L, 1L));
    }

    @Test
    void redeemRewardThrowsWhenInsufficientPoints() {
        RewardAccount account = RewardAccount.builder().userId(1L).points(50).tier(RewardAccount.Tier.GOLD).build();
        RewardItem item = RewardItem.builder().id(1L).active(true).stock(1).pointsRequired(100).type(RewardItem.ItemType.COUPON).build();
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        when(itemRepo.findById(1L)).thenReturn(Optional.of(item));

        assertThrows(RewardException.class, () -> rewardCommandService.redeemReward(1L, 1L));
    }

    @Test
    void redeemRewardThrowsWhenDailyLimitExceeded() {
        RewardAccount account = RewardAccount.builder().userId(1L).points(1000).tier(RewardAccount.Tier.GOLD).build();
        RewardItem item = RewardItem.builder().id(1L).active(true).stock(1).pointsRequired(200).type(RewardItem.ItemType.COUPON).build();
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        when(itemRepo.findById(1L)).thenReturn(Optional.of(item));
        when(txnRepo.sumRedeemedPointsToday(eq(1L), eq(RewardTransaction.TxnType.REDEEM), any(), any())).thenReturn(4900);

        assertThrows(RewardException.class, () -> rewardCommandService.redeemReward(1L, 1L));
    }

    @Test
    void redeemRewardCouponGeneratesCouponCodeAndReducesStock() {
        RewardAccount account = RewardAccount.builder().userId(1L).points(1000).tier(RewardAccount.Tier.GOLD).build();
        RewardItem item = RewardItem.builder().id(1L).name("Coupon").active(true).stock(2).pointsRequired(200).type(RewardItem.ItemType.COUPON).build();
        Redemption saved = Redemption.builder().id(1L).userId(1L).rewardId(1L).pointsUsed(200).status(Redemption.RedemptionStatus.COMPLETED).build();
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        when(itemRepo.findById(1L)).thenReturn(Optional.of(item));
        when(txnRepo.sumRedeemedPointsToday(eq(1L), eq(RewardTransaction.TxnType.REDEEM), any(), any())).thenReturn(0);
        when(redemptionRepo.save(any(Redemption.class))).thenReturn(saved);

        Redemption redemption = rewardCommandService.redeemReward(1L, 1L);

        assertEquals(800, account.getPoints());
        assertEquals(1, item.getStock());
        verify(redemptionRepo).save(any(Redemption.class));
        assertEquals(1L, redemption.getId());
    }

    @Test
    void redeemRewardCashbackCreditsWallet() {
        RewardAccount account = RewardAccount.builder().userId(1L).points(1000).tier(RewardAccount.Tier.GOLD).build();
        RewardItem item = RewardItem.builder().id(1L).name("Cashback").active(true).stock(2).pointsRequired(200).type(RewardItem.ItemType.CASHBACK).cashbackAmount(new BigDecimal("25")).build();
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        when(itemRepo.findById(1L)).thenReturn(Optional.of(item));
        when(txnRepo.sumRedeemedPointsToday(eq(1L), eq(RewardTransaction.TxnType.REDEEM), any(), any())).thenReturn(0);
        when(walletClient.credit(1L, new BigDecimal("25"))).thenReturn(ResponseEntity.ok().build());
        when(redemptionRepo.save(any(Redemption.class))).thenAnswer(invocation -> invocation.getArgument(0));

        rewardCommandService.redeemReward(1L, 1L);

        verify(walletClient).credit(1L, new BigDecimal("25"));
    }

    @Test
    void getSummaryReturnsNextTierProgress() {
        RewardAccount account = RewardAccount.builder().userId(1L).points(800).tier(RewardAccount.Tier.SILVER).build();
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(account));

        RewardSummaryDto summary = rewardQueryService.getSummary(1L);

        assertEquals("GOLD", summary.getNextTier());
        assertEquals(200, summary.getPointsToNextTier());
    }

    @Test
    void getCatalogDelegatesToRepository() {
        when(itemRepo.findByActiveTrueOrderByPointsRequiredAsc()).thenReturn(List.of(RewardItem.builder().id(1L).build()));

        assertEquals(1, rewardQueryService.getCatalog().size());
    }

    @Test
    void getTransactionsDelegatesToRepository() {

        RewardTransaction txn = RewardTransaction.builder()
                .id(1L)
                .userId(1L)
                .build();

        Page<RewardTransaction> mockPage = new PageImpl<>(List.of(txn));

        when(txnRepo.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(mockPage);

        Pageable pageable = PageRequest.of(0, 5);

        PageResponse<RewardTransaction> result =
                rewardQueryService.getTransactions(1L, pageable);

        assertEquals(1, result.getContent().size());

        verify(txnRepo)
                .findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class));
    }

    @Test
    void createAccountIfNotExistsCreatesWhenMissing() {
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.empty());

        rewardCommandService.createAccountIfNotExists(1L);

        verify(rewardRepo).save(any(RewardAccount.class));
    }

    @Test
    void createAccountIfNotExistsSkipsWhenPresent() {
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(RewardAccount.builder().userId(1L).build()));

        rewardCommandService.createAccountIfNotExists(1L);

        verify(rewardRepo, never()).save(any(RewardAccount.class));
    }
}
