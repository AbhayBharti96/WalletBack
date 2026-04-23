package com.loyaltyService.reward_service.repository;

import com.loyaltyService.reward_service.entity.RewardItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface RewardItemRepository extends JpaRepository<RewardItem, Long> {
    List<RewardItem> findByActiveTrueOrderByPointsRequiredAsc();

    @Query("""
            select r from RewardItem r
            where r.active = true
              and (r.activeFrom is null or r.activeFrom <= :now)
              and (r.activeUntil is null or r.activeUntil >= :now)
            order by r.pointsRequired asc
            """)
    List<RewardItem> findVisibleCatalog(@Param("now") LocalDateTime now);
}
