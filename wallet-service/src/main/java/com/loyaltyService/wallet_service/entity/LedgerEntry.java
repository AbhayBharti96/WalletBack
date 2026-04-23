package com.loyaltyService.wallet_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Append-only ledger record. Every balance change MUST produce a LedgerEntry first. */
@Entity @Table(name = "ledger_entries",
    indexes = { @Index(name = "idx_ledger_user", columnList = "user_id"),
                @Index(name = "idx_ledger_ref",  columnList = "reference_id") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LedgerEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Enumerated(EnumType.STRING) @Column(name = "type", nullable = false, length = 10) private EntryType type;
    @Column(name = "amount", nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(name = "reference_id", nullable = false, length = 64) private String referenceId;
    @Column(name = "description", length = 255) private String description;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    public enum EntryType { CREDIT, DEBIT }
}
