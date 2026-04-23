package com.loyaltyService.wallet_service.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TopupRequest {
    @NotNull @DecimalMin("1.00") @DecimalMax("50000.00")
    private BigDecimal amount;
    /** Caller-provided idempotency key — prevents duplicate topups on retry */
    @Size(max = 64)
    private String idempotencyKey;
}
