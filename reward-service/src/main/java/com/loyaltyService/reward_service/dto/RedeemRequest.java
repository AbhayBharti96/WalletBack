package com.loyaltyService.reward_service.dto;
import jakarta.validation.constraints.NotNull;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RedeemRequest {
    @NotNull(message = "Reward ID is required") private Long rewardId;
}
