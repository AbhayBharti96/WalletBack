package com.loyaltyService.reward_service.repository;

import com.loyaltyService.reward_service.entity.RewardAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface RewardRepository extends JpaRepository<RewardAccount, Long> {
Optional<RewardAccount> findByUserId(Long userId);
}
