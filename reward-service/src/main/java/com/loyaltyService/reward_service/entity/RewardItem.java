package com.loyaltyService.reward_service.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name = "reward_catalog")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RewardItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "name", nullable = false, length = 100) private String name;
    @Column(name = "description", length = 255) private String description;
    @Column(name = "points_required", nullable = false) private Integer pointsRequired;
    @Enumerated(EnumType.STRING) @Column(name = "type", nullable = false, length = 20) private ItemType type;
    @Column(name = "cashback_amount", precision = 10, scale = 2) private BigDecimal cashbackAmount;
    @Column(name = "tier_required", length = 20) private String tierRequired;
    @Column(name = "stock", nullable = false) @Builder.Default private Integer stock = 100;
    @Column(name = "active", nullable = false) @Builder.Default private Boolean active = true;
    @Column(name = "active_from") private LocalDateTime activeFrom;
    @Column(name = "active_until") private LocalDateTime activeUntil;
    public enum ItemType { CASHBACK, COUPON, VOUCHER }
}
