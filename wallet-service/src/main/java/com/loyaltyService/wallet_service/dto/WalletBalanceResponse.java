package com.loyaltyService.wallet_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletBalanceResponse {
    private Long userId;
    private BigDecimal balance;
    private String status;
    private LocalDateTime lastUpdated;
}
