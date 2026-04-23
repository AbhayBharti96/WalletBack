package com.loyaltyService.reward_service.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name = "reward_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RewardTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "points", nullable = false) private Integer points;
    @Enumerated(EnumType.STRING) @Column(name = "type", nullable = false, length = 20) private TxnType type;
    @Column(name = "description", length = 255) private String description;
    @Column(name = "expiry_date") private LocalDateTime expiryDate;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    public enum TxnType { EARN, BONUS, REDEEM, EXPIRE }
}
