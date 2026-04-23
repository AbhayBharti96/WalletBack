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
import com.loyaltyService.reward_service.service.RewardQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CQRS — Query implementation for Reward.
 * Handles all read (non-state-changing) operations.
 * Results for reward summary and catalog are cached in Redis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RewardQueryServiceImpl implements RewardQueryService {

    private final RewardRepository rewardRepo;
    private final RewardItemRepository itemRepo;
    private final RewardTransactionRepository txnRepo;

    @Value("${rewards.tiers.gold-threshold:1000}")
    private int goldThreshold;
    @Value("${rewards.tiers.platinum-threshold:5000}")
    private int platinumThreshold;

    @Override
    @Transactional
    @Cacheable(value = "reward-summary", key = "#userId")
    public RewardSummaryDto getSummary(Long userId) {
        log.debug("Cache miss — loading reward summary from DB for userId={}", userId);
        RewardAccount acc = findAccount(userId);
        RewardAccount.Tier computedTier = computeTier(acc.getPoints());
        if (acc.getTier() != computedTier) {
            acc.setTier(computedTier);
            rewardRepo.save(acc);
        }
        String nextTier;
        int needed;
        if (acc.getPoints() < goldThreshold) {
            nextTier = "GOLD";
            needed = goldThreshold - acc.getPoints();
        } else if (acc.getPoints() < platinumThreshold) {
            nextTier = "PLATINUM";
            needed = platinumThreshold - acc.getPoints();
        } else {
            nextTier = "PLATINUM (MAX)";
            needed = 0;
        }
        return RewardSummaryDto.builder()
                .userId(userId)
                .points(acc.getPoints())
                .tier(computedTier.name())
                .nextTier(nextTier)
                .pointsToNextTier(needed)
                .build();
    }

    @Override
    @Cacheable(value = "reward-catalog")
    public List<RewardItem> getCatalog() {
        log.debug("Cache miss — loading reward catalog from DB");
        return itemRepo.findVisibleCatalog(LocalDateTime.now());
    }

    @Override
    public List<RewardItem> getCatalogForAdmin() {
        return itemRepo.findAll(Sort.by(Sort.Direction.ASC, "pointsRequired"));
    }

    @Override
    public PageResponse<RewardTransaction> getTransactions(Long userId, Pageable pageable) {
        Page<RewardTransaction> page =
                txnRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RewardAccount findAccount(Long userId) {
        return rewardRepo.findByUserId(userId)
                .orElseThrow(() -> new RewardException("Reward account not found", HttpStatus.NOT_FOUND));
    }

    private RewardAccount.Tier computeTier(Integer points) {
        int safePoints = points == null ? 0 : points;
        if (safePoints >= platinumThreshold) {
            return RewardAccount.Tier.PLATINUM;
        }
        if (safePoints >= goldThreshold) {
            return RewardAccount.Tier.GOLD;
        }
        return RewardAccount.Tier.SILVER;
    }
}
