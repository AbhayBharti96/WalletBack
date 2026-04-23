package com.loyaltyService.wallet_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "transactions",
    indexes = { @Index(name = "idx_txn_sender",   columnList = "sender_id"),
                @Index(name = "idx_txn_receiver", columnList = "receiver_id"),
                @Index(name = "idx_txn_ref",      columnList = "reference_id") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "sender_id") private Long senderId;
    @Column(name = "receiver_id") private Long receiverId;
    @Column(name = "amount", nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 20)
    @Builder.Default private TxnStatus status = TxnStatus.PENDING;
    @Enumerated(EnumType.STRING) @Column(name = "type", nullable = false, length = 20) private TxnType type;
    @Column(name = "reference_id", unique = true, nullable = false, length = 64) private String referenceId;
    @Column(name = "idempotency_key", unique = true, length = 64) private String idempotencyKey;
    @Column(name = "description", length = 255) private String description;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    public enum TxnStatus { PENDING, SUCCESS, FAILED, CANCELLED, REVERSED }
    public enum TxnType   { TOPUP, TRANSFER, WITHDRAW, CASHBACK, REDEEM }
}
