package com.loyaltyService.wallet_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** ✅ FIXED: BigDecimal balance + enum status + @Version optimistic lock */
@Entity @Table(name = "wallet_accounts",
    indexes = @Index(name = "idx_wallet_user_id", columnList = "user_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", unique = true, nullable = false) private Long userId;
    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    @Builder.Default private BigDecimal balance = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 20)
    @Builder.Default private WalletStatus status = WalletStatus.ACTIVE;
    @Version private Integer version;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
    public enum WalletStatus { ACTIVE, BLOCKED, SUSPENDED }
    public boolean isActive() { return this.status == WalletStatus.ACTIVE; }
    public void credit(BigDecimal amount) { this.balance = this.balance.add(amount); }
    public void debit(BigDecimal amount)  { this.balance = this.balance.subtract(amount); }
}
