package com.loyaltyService.reward_service.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name = "redemptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Redemption {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "reward_id", nullable = false) private Long rewardId;
    @Column(name = "points_used", nullable = false) private Integer pointsUsed;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 20)
    @Builder.Default private RedemptionStatus status = RedemptionStatus.COMPLETED;
    @Column(name = "coupon_code", length = 50) private String couponCode;
    @Column(name = "cashback_amount") private BigDecimal cashbackAmount;
    @CreationTimestamp @Column(name = "redeemed_at", updatable = false) private LocalDateTime redeemedAt;
    public enum RedemptionStatus { COMPLETED, REVERSED }
}
