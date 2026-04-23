package com.loyaltyService.reward_service.dto;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RewardSummaryDto {
    private Long userId;
    private Integer points;
    private String tier;
    private String nextTier;
    private Integer pointsToNextTier;
}
