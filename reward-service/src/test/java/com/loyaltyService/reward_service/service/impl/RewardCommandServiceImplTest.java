package com.loyaltyService.reward_service.service.impl;

import com.loyaltyService.reward_service.client.WalletClient;
import com.loyaltyService.reward_service.dto.RewardItemRequest;
import com.loyaltyService.reward_service.entity.RewardAccount;
import com.loyaltyService.reward_service.entity.RewardItem;
import com.loyaltyService.reward_service.exception.RewardException;
import com.loyaltyService.reward_service.repository.RedemptionRepository;
import com.loyaltyService.reward_service.repository.RewardItemRepository;
import com.loyaltyService.reward_service.repository.RewardRepository;
import com.loyaltyService.reward_service.repository.RewardTransactionRepository;
import com.loyaltyService.reward_service.service.KafkaProducerService;
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
class RewardCommandServiceImplTest {

    @Mock
    private RewardRepository rewardRepo;
    @Mock
    private RewardTransactionRepository txnRepo;
    @Mock
    private RewardItemRepository itemRepo;
    @Mock
    private RedemptionRepository redemptionRepo;
    @Mock
    private WalletClient walletClient;
    @Mock
    private KafkaProducerService kafkaProducer;

    @InjectMocks
    private RewardCommandServiceImpl commandService;

    private RewardAccount testAccount;

    @BeforeEach
    void setUp() {
        testAccount = RewardAccount.builder()
                .userId(1L).points(1000).tier(RewardAccount.Tier.SILVER).firstTopupDone(true)
                .build();

        ReflectionTestUtils.setField(commandService, "pointsPerRupee", 100);
        ReflectionTestUtils.setField(commandService, "minRedeemPoints", 100);
        ReflectionTestUtils.setField(commandService, "maxDailyRedeemPoints", 5000);
        ReflectionTestUtils.setField(commandService, "goldThreshold", 1000);
        ReflectionTestUtils.setField(commandService, "platinumThreshold", 5000);
        ReflectionTestUtils.setField(commandService, "firstTopupBonus", 100);
    }

    @Test
    void testEarnPoints_ExistingAccount() {
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(testAccount));

        commandService.earnPoints(1L, new BigDecimal("5000")); // 50 points

        assertEquals(1050, testAccount.getPoints());
        assertEquals(RewardAccount.Tier.GOLD, testAccount.getTier()); // Crosses 1000 threshold
        verify(txnRepo, atLeastOnce()).save(any());
        verify(rewardRepo, times(1)).save(testAccount);
        verify(kafkaProducer, times(1)).send(eq("reward-events"), anyMap());
    }

    @Test
    void testRedeemPoints_Success() {
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(testAccount));
        when(txnRepo.sumRedeemedPointsToday(any(), any(), any(), any())).thenReturn(0);

        commandService.redeemPoints(1L, 500); // 5 rupees

        assertEquals(500, testAccount.getPoints());
        verify(walletClient, times(1)).credit(eq(1L), eq(new BigDecimal("5")));
        verify(txnRepo, times(1)).save(any());
        verify(rewardRepo, times(1)).save(testAccount);
    }

    @Test
    void testRedeemPoints_WalletFailureCompensation() {
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(testAccount));
        when(txnRepo.sumRedeemedPointsToday(any(), any(), any(), any())).thenReturn(0);
        
        doThrow(new RuntimeException("API Failure")).when(walletClient).credit(any(), any());

        assertThrows(RewardException.class, () -> commandService.redeemPoints(1L, 500));

        // State is restored
        assertEquals(1000, testAccount.getPoints());
        // Verify compensation kafka event
        verify(kafkaProducer, times(1)).send(eq("reward-events"), argThat(m -> 
            "POINTS_REDEEM_FAILED".equals(((java.util.Map<?, ?>) m).get("event"))
        ));
    }

    @Test
    void addCatalogItemRequiresCashbackAmountForCashbackRewards() {
        RewardItemRequest request = new RewardItemRequest();
        request.setName("Cashback reward");
        request.setType("CASHBACK");
        request.setPointsRequired(100);
        request.setStock(10);

        assertThrows(RewardException.class, () -> commandService.addCatalogItem(request));
        verify(itemRepo, never()).save(any());
    }

    @Test
    void addCatalogItemRequiresPointsForVouchersAndCoupons() {
        RewardItemRequest request = new RewardItemRequest();
        request.setName("Gift voucher");
        request.setType("VOUCHER");
        request.setStock(10);

        assertThrows(RewardException.class, () -> commandService.addCatalogItem(request));
        verify(itemRepo, never()).save(any());
    }

    @Test
    void addCatalogItemClearsCashbackAmountForCouponRewards() {
        RewardItemRequest request = new RewardItemRequest();
        request.setName("Coupon reward");
        request.setType("COUPON");
        request.setPointsRequired(100);
        request.setStock(10);
        request.setCashbackAmount(new BigDecimal("25"));

        when(itemRepo.save(any(RewardItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RewardItem saved = commandService.addCatalogItem(request);

        assertEquals(RewardItem.ItemType.COUPON, saved.getType());
        assertNull(saved.getCashbackAmount());
    }
}
