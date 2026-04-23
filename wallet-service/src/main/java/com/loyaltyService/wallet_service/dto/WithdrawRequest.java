package com.loyaltyService.wallet_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WithdrawRequest {
    @NotNull @DecimalMin("1.00") private BigDecimal amount;
}
