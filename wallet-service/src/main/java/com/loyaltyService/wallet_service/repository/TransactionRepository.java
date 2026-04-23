package com.loyaltyService.wallet_service.repository;

import com.loyaltyService.wallet_service.entity.Transaction;
import com.loyaltyService.wallet_service.entity.Transaction.TxnStatus;
import com.loyaltyService.wallet_service.entity.Transaction.TxnType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findBySenderIdOrReceiverIdOrderByCreatedAtDesc(
            Long senderId,
            Long receiverId,
            Pageable pageable
    );

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> findByReferenceId(String referenceId);

    // ✅ FIXED
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.receiverId = :userId
        AND t.type = :type
        AND t.status = :status
        AND t.createdAt BETWEEN :start AND :end
    """)
    BigDecimal sumTodayTopups(
            @Param("userId") Long userId,
            @Param("type") TxnType type,
            @Param("status") TxnStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // ✅ FIXED
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.senderId = :userId
        AND t.type = :type
        AND t.status = :status
        AND t.createdAt BETWEEN :start AND :end
    """)
    BigDecimal sumTodayTransfers(
            @Param("userId") Long userId,
            @Param("type") TxnType type,
            @Param("status") TxnStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // ✅ FIXED
    @Query("""
        SELECT COUNT(t)
        FROM Transaction t
        WHERE t.senderId = :userId
        AND t.type = :type
        AND t.status = :status
        AND t.createdAt BETWEEN :start AND :end
    """)
    long countTodayTransfers(
            @Param("userId") Long userId,
            @Param("type") TxnType type,
            @Param("status") TxnStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT t
        FROM Transaction t
        WHERE (t.senderId = :userId OR t.receiverId = :userId)
        AND t.createdAt BETWEEN :from AND :to
        ORDER BY t.createdAt DESC
    """)
    List<Transaction> findStatement(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
