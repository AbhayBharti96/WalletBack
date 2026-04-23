package com.loyaltyService.wallet_service.repository;

import com.loyaltyService.wallet_service.entity.WalletAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface WalletAccountRepository extends JpaRepository<WalletAccount, Long> {
    Optional<WalletAccount> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    @Modifying
    @Query("UPDATE WalletAccount w SET w.status = :status WHERE w.userId = :userId")
    void updateStatus(@Param("userId") Long userId, @Param("status") WalletAccount.WalletStatus status);
}
