package com.loyaltyService.wallet_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;


import java.math.BigDecimal;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Payment {

    @Id
    private String orderId; // keep as is

    private String paymentId; // ✅ ADD THIS

    private Long userId;
    private BigDecimal amount;
    private String status; // CREATED, SUCCESS, FAILED
}