package com.loyaltyService.auth_service.repository;


import com.loyaltyService.auth_service.model.OtpStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpStoreRepository extends JpaRepository<OtpStore, Long> {

    Optional<OtpStore> findTopByPhoneAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(
            String phone, OtpStore.OtpType otpType);  // ✅ IsUsed → Used

    Optional<OtpStore> findTopByEmailAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(
            String email, OtpStore.OtpType otpType);  // ✅ IsUsed → Used

    @Query("SELECT COUNT(o) FROM OtpStore o WHERE o.phone = :phone AND o.otpType = :type AND o.createdAt > :since")
    long countRecentOtpsByPhone(@Param("phone") String phone,
                                 @Param("type") OtpStore.OtpType type,
                                 @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(o) FROM OtpStore o WHERE o.email = :email AND o.otpType = :type AND o.createdAt > :since")
    long countRecentOtpsByEmail(@Param("email") String email,
                                 @Param("type") OtpStore.OtpType type,
                                 @Param("since") LocalDateTime since);

    @Modifying
    @Transactional
    @Query("DELETE FROM OtpStore o WHERE o.expiryTime < :now")
    void deleteExpiredOtps(@Param("now") LocalDateTime now);
}