package com.loyaltyService.reward_service.service.impl;

import com.loyaltyService.reward_service.dto.PageResponse;
import com.loyaltyService.reward_service.dto.RewardSummaryDto;
import com.loyaltyService.reward_service.entity.RewardAccount;
import com.loyaltyService.reward_service.entity.RewardItem;
import com.loyaltyService.reward_service.entity.RewardTransaction;
import com.loyaltyService.reward_service.exception.RewardException;
import com.loyaltyService.reward_service.repository.RewardItemRepository;
import com.loyaltyService.reward_service.repository.RewardRepository;
import com.loyaltyService.reward_service.repository.RewardTransactionRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardQueryServiceImplTest {

    @Mock
    private RewardRepository rewardRepo;

    @Mock
    private RewardItemRepository itemRepo;

    @Mock
    private RewardTransactionRepository txnRepo;

    @InjectMocks
    private RewardQueryServiceImpl queryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(queryService, "goldThreshold", 1000);
        ReflectionTestUtils.setField(queryService, "platinumThreshold", 5000);
    }

    @Test
    void testGetSummary_Silver() {
        RewardAccount acc = RewardAccount.builder().userId(1L).points(500).tier(RewardAccount.Tier.SILVER).build();
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(acc));

        RewardSummaryDto summary = queryService.getSummary(1L);

        assertNotNull(summary);
        assertEquals(500, summary.getPoints());
        assertEquals("SILVER", summary.getTier());
        assertEquals("GOLD", summary.getNextTier());
        assertEquals(500, summary.getPointsToNextTier()); // 1000 - 500
    }

    @Test
    void testGetSummary_Gold() {
        RewardAccount acc = RewardAccount.builder().userId(1L).points(1500).tier(RewardAccount.Tier.GOLD).build();
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(acc));

        RewardSummaryDto summary = queryService.getSummary(1L);

        assertEquals("GOLD", summary.getTier());
        assertEquals("PLATINUM", summary.getNextTier());
        assertEquals(3500, summary.getPointsToNextTier()); // 5000 - 1500
    }

    @Test
    void testGetSummary_Platinum() {
        RewardAccount acc = RewardAccount.builder().userId(1L).points(6000).tier(RewardAccount.Tier.PLATINUM).build();
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.of(acc));

        RewardSummaryDto summary = queryService.getSummary(1L);

        assertEquals("PLATINUM", summary.getTier());
        assertEquals("PLATINUM (MAX)", summary.getNextTier());
        assertEquals(0, summary.getPointsToNextTier());
    }

    @Test
    void testGetSummary_NotFound() {
        when(rewardRepo.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(RewardException.class, () -> queryService.getSummary(1L));
    }

    @Test
    void testGetCatalog() {
        RewardItem item = RewardItem.builder().id(1L).name("Test Item").pointsRequired(100).build();
        when(itemRepo.findByActiveTrueOrderByPointsRequiredAsc()).thenReturn(List.of(item));

        List<RewardItem> catalog = queryService.getCatalog();

        assertFalse(catalog.isEmpty());
        assertEquals(1, catalog.size());
    }

    @Test
    void testGetTransactions() {

        RewardTransaction txn = RewardTransaction.builder()
                .id(1L)
                .userId(1L)
                .points(10)
                .build();

        Page<RewardTransaction> page = new PageImpl<>(List.of(txn));

        when(txnRepo.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        Pageable pageable = PageRequest.of(0, 5);

        PageResponse<RewardTransaction> result =
                queryService.getTransactions(1L, pageable);

        assertNotNull(result);
        assertFalse(result.getContent().isEmpty());
        assertEquals(1, result.getContent().size());

        // Optional strong assertions 🔥
        assertEquals(0, result.getPage());
        assertEquals(5, result.getSize());
        assertEquals(1, result.getTotalElements());

        verify(txnRepo, times(1))
                .findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class));
    }
}
