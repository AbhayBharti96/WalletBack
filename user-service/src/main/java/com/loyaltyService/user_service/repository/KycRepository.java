package com.loyaltyService.user_service.repository;

import com.loyaltyService.user_service.entity.KycDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface KycRepository extends JpaRepository<KycDetail, Long> {

    Optional<KycDetail> findFirstByUserIdOrderBySubmittedAtDesc(Long userId);

    // All KYC by status — used by admin pending list
    Page<KycDetail> findByStatusOrderBySubmittedAtDesc(KycDetail.KycStatus status, Pageable pageable);

    List<KycDetail> findByStatus(KycDetail.KycStatus status);

    boolean existsByUserIdAndStatus(Long userId, KycDetail.KycStatus status);

    long countByStatus(KycDetail.KycStatus status);

    // Find latest PENDING kyc for a specific user — used by admin "approve by userId"
    Optional<KycDetail> findFirstByUserIdAndStatusOrderBySubmittedAtDesc(
            Long userId, KycDetail.KycStatus status);

    @Query("SELECT k FROM KycDetail k JOIN FETCH k.user WHERE k.user.id = :userId ORDER BY k.submittedAt DESC")
    List<KycDetail> findAllByUserId(@Param("userId") Long userId);

    // Admin needs full user info alongside KYC — fetch with user join to avoid N+1
    @Query("SELECT k FROM KycDetail k JOIN FETCH k.user WHERE k.status = :status ORDER BY k.submittedAt ASC")
    List<KycDetail> findByStatusWithUser(@Param("status") KycDetail.KycStatus status);

    // Count approved/rejected today — for dashboard
    @Query("SELECT COUNT(k) FROM KycDetail k WHERE k.status = :status AND k.updatedAt >= :since")
    long countByStatusSince(@Param("status") KycDetail.KycStatus status,
                            @Param("since") java.time.Instant since);
}
