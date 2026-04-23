package com.loyaltyService.reward_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class RewardItemRequest {
    private String name;
    private String description;
    private Integer pointsRequired;
    private String type;           // must match RewardItem.ItemType enum: CASHBACK | COUPON | VOUCHER
    private Integer stock;
    private String tierRequired;   // SILVER | GOLD | PLATINUM
    private BigDecimal cashbackAmount;
    private Boolean active;
    private LocalDateTime activeFrom;
    private LocalDateTime activeUntil;

    public RewardItemRequest(
            String name,
            String description,
            Integer pointsRequired,
            String type,
            Integer stock,
            String tierRequired,
            BigDecimal cashbackAmount) {
        this(name, description, pointsRequired, type, stock, tierRequired, cashbackAmount, true, null, null);
    }

    public RewardItemRequest(
            String name,
            String description,
            Integer pointsRequired,
            String type,
            Integer stock,
            String tierRequired,
            BigDecimal cashbackAmount,
            Boolean active,
            LocalDateTime activeFrom,
            LocalDateTime activeUntil) {
        this.name = name;
        this.description = description;
        this.pointsRequired = pointsRequired;
        this.type = type;
        this.stock = stock;
        this.tierRequired = tierRequired;
        this.cashbackAmount = cashbackAmount;
        this.active = active;
        this.activeFrom = activeFrom;
        this.activeUntil = activeUntil;
    }
}
