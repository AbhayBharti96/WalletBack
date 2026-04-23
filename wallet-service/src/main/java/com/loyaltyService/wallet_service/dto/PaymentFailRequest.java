package com.loyaltyService.wallet_service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class PaymentFailRequest {
    @JsonAlias({"razorpay_order_id", "orderId"})
    private String razorpayOrderId;
    private String reason;
}
