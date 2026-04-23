package com.loyaltyService.wallet_service.repository;

import com.loyaltyService.wallet_service.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    Page<LedgerEntry> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
