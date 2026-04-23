package com.loyaltyService.wallet_service.service;

import com.loyaltyService.wallet_service.dto.PaymentVerifyRequest;
import com.loyaltyService.wallet_service.entity.Payment;
import com.loyaltyService.wallet_service.repository.PaymentRepository;
import com.loyaltyService.wallet_service.service.impl.RazorpayServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RazorpayServiceTest {

    @Mock
    private PaymentRepository paymentRepo;

    @Mock
    private WalletCommandService walletCommandService;

    @Mock
    private WalletQueryService walletQueryService;

    @Mock
    private KafkaProducerService kafkaProducer;

    @InjectMocks
    private RazorpayServiceImpl razorpayService;

    @Test
    void createOrderWithoutCredentialsThrows() {
        ReflectionTestUtils.setField(razorpayService, "key", null);
        ReflectionTestUtils.setField(razorpayService, "secret", null);

        assertThrows(Exception.class, () -> razorpayService.createOrder(1L, new BigDecimal("100.00")));
        verify(walletCommandService).createWallet(1L);
    }

    @Test
    void verifyPaymentRejectsOrderOwnedByDifferentUser() {
        Payment payment = Payment.builder()
                .orderId("order_123")
                .userId(2L)
                .amount(new BigDecimal("100.00"))
                .status("CREATED")
                .build();
        when(paymentRepo.findById("order_123")).thenReturn(Optional.of(payment));

        PaymentVerifyRequest request = new PaymentVerifyRequest();
        request.setRazorpayOrderId("order_123");
        request.setRazorpayPaymentId("pay_123");
        request.setRazorpaySignature("sig");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> razorpayService.verifyPayment(1L, request));

        assertEquals("Payment does not belong to authenticated user", ex.getMessage());
        verify(paymentRepo, never()).save(payment);
        verifyNoInteractions(walletCommandService, walletQueryService, kafkaProducer);
    }
}
