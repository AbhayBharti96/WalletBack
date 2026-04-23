package com.loyaltyService.wallet_service.service.impl;

import com.loyaltyService.wallet_service.entity.Payment;
import com.loyaltyService.wallet_service.entity.Transaction;
import com.loyaltyService.wallet_service.repository.PaymentRepository;
import com.loyaltyService.wallet_service.repository.TransactionRepository;
import com.loyaltyService.wallet_service.service.KafkaProducerService;
import com.loyaltyService.wallet_service.service.WalletCommandService;
import com.loyaltyService.wallet_service.service.WalletQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RazorpayServiceImplTest {

    @Mock
    private PaymentRepository paymentRepo;
    @Mock
    private TransactionRepository transactionRepo;
    @Mock
    private WalletCommandService walletCommandService;
    @Mock
    private WalletQueryService walletQueryService;
    @Mock
    private KafkaProducerService kafkaProducer;

    @InjectMocks
    private RazorpayServiceImpl razorpayService;

    @Test
    void cancelPayment_recordsCancelledTopupTransaction() {
        Payment payment = Payment.builder()
                .orderId("order_123")
                .userId(100L)
                .amount(new BigDecimal("250.00"))
                .status("CREATED")
                .build();

        when(paymentRepo.findById("order_123")).thenReturn(Optional.of(payment));
        when(transactionRepo.findByReferenceId("CANCELLED_order_123")).thenReturn(Optional.empty());

        razorpayService.cancelPayment(100L, "order_123");

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepo).save(transactionCaptor.capture());

        Transaction transaction = transactionCaptor.getValue();
        assertEquals(100L, transaction.getReceiverId());
        assertEquals(new BigDecimal("250.00"), transaction.getAmount());
        assertEquals(Transaction.TxnStatus.CANCELLED, transaction.getStatus());
        assertEquals(Transaction.TxnType.TOPUP, transaction.getType());
        assertEquals("CANCELLED_order_123", transaction.getReferenceId());
        assertEquals("CANCELLED_order_123", transaction.getIdempotencyKey());
        assertEquals("CANCELLED", payment.getStatus());
        verify(paymentRepo).save(payment);
        verify(kafkaProducer).send(eq("payment-events"), anyMap());
    }

    @Test
    void cancelPayment_rejectsSuccessfulPayment() {
        Payment payment = Payment.builder()
                .orderId("order_123")
                .userId(100L)
                .amount(new BigDecimal("250.00"))
                .status("SUCCESS")
                .build();

        when(paymentRepo.findById("order_123")).thenReturn(Optional.of(payment));

        assertThrows(RuntimeException.class, () -> razorpayService.cancelPayment(100L, "order_123"));
        verify(transactionRepo, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
