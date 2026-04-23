package com.loyaltyService.reward_service.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name = "reward_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RewardAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", unique = true, nullable = false) private Long userId;
    @Column(name = "points", nullable = false) @Builder.Default private Integer points = 0;
    @Enumerated(EnumType.STRING) @Column(name = "tier", nullable = false, length = 20)
    @Builder.Default private Tier tier = Tier.SILVER;
    @Column(name = "first_topup_done") @Builder.Default private Boolean firstTopupDone = false;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
    public enum Tier { SILVER, GOLD, PLATINUM }
}
