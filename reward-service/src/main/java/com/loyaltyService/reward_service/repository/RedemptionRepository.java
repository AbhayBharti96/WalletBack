package com.loyaltyService.reward_service.repository;

import com.loyaltyService.reward_service.entity.Redemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface RedemptionRepository extends JpaRepository<Redemption, Long> {
    List<Redemption> findByUserIdOrderByRedeemedAtDesc(Long userId);
    boolean existsByUserIdAndRewardId(Long userId, Long rewardId);
}
