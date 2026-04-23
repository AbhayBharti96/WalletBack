package com.loyaltyService.reward_service.repository;

import com.loyaltyService.reward_service.entity.RewardTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface RewardTransactionRepository extends JpaRepository<RewardTransaction, Long> {
    Page<RewardTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    @Query("SELECT t FROM RewardTransaction t WHERE t.userId = :userId AND t.expiryDate BETWEEN :now AND :soon")
    List<RewardTransaction> findExpiringPoints(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now,
            @Param("soon") LocalDateTime soon);

    /**
     * FIX: New query — sums points redeemed by this user today.
     * Used in RewardService to enforce the daily redemption cap.
     * COALESCE ensures 0 is returned when no rows exist (avoids NullPointerException).
     */
    @Query("""
        SELECT COALESCE(SUM(t.points), 0)
        FROM RewardTransaction t
        WHERE t.userId = :userId
          AND t.type   = :type
          AND t.createdAt BETWEEN :start AND :end
    """)
    int sumRedeemedPointsToday(
            @Param("userId") Long userId,
            @Param("type")   RewardTransaction.TxnType type,
            @Param("start")  LocalDateTime start,
            @Param("end")    LocalDateTime end);
}
