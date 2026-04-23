package com.loyaltyService.reward_service.service;

import com.loyaltyService.reward_service.dto.RewardItemRequest;
import com.loyaltyService.reward_service.entity.Redemption;
import com.loyaltyService.reward_service.entity.RewardItem;

import java.math.BigDecimal;

/**
 * CQRS — Command side: all write operations for Reward.
 */
public interface RewardCommandService {

    void earnPoints(Long userId, BigDecimal amount);

    void redeemPoints(Long userId, Integer points);

    Redemption redeemReward(Long userId, Long rewardId);

    void convertPointsToCash(Long userId, Integer points);

    RewardItem addCatalogItem(RewardItemRequest req);

    RewardItem updateCatalogItem(Long rewardId, RewardItemRequest req);

    void deleteCatalogItem(Long rewardId);

    void createAccountIfNotExists(Long userId);
}
