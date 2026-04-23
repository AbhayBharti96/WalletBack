package com.loyaltyService.wallet_service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentVerifyRequest {
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
}