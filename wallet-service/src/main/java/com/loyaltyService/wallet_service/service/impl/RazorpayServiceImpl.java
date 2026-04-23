package com.loyaltyService.wallet_service.service.impl;

import com.loyaltyService.wallet_service.dto.PaymentVerifyRequest;
import com.loyaltyService.wallet_service.entity.Payment;
import com.loyaltyService.wallet_service.entity.Transaction;
import com.loyaltyService.wallet_service.repository.PaymentRepository;
import com.loyaltyService.wallet_service.repository.TransactionRepository;
import com.loyaltyService.wallet_service.service.KafkaProducerService;
import com.loyaltyService.wallet_service.service.RazorpayService;
import com.loyaltyService.wallet_service.service.WalletCommandService;
import com.loyaltyService.wallet_service.service.WalletQueryService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RazorpayServiceImpl implements RazorpayService {

    private final PaymentRepository paymentRepo;
    private final TransactionRepository transactionRepo;
    private final WalletCommandService walletCommandService;
    private final WalletQueryService walletQueryService;
    private final KafkaProducerService kafkaProducer;

    @Value("${razorpay.key}")
    private String key;

    @Value("${razorpay.secret}")
    private String secret;

    @Override
    public Order createOrder(Long userId, BigDecimal amount) throws RazorpayException {
        walletCommandService.createWallet(userId);

        RazorpayClient client = new RazorpayClient(key, secret);

        JSONObject options = new JSONObject();
        options.put("amount", amount.multiply(BigDecimal.valueOf(100)));
        options.put("currency", "INR");
        options.put("receipt", "wallet_" + System.currentTimeMillis());

        Order order = client.orders.create(options);

        String orderId = order.get("id");
        paymentRepo.save(
                Payment.builder()
                        .orderId(orderId)
                        .userId(userId)
                        .amount(amount)
                        .status("CREATED")
                        .build()
        );
        walletCommandService.recordPendingTopup(userId, amount, orderId);

        return order;
    }

    @Override
    public void verifyPayment(Long userId, PaymentVerifyRequest req) throws RazorpayException {
        Payment payment = paymentRepo.findById(req.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!userId.equals(payment.getUserId())) {
            throw new RuntimeException("Payment does not belong to authenticated user");
        }

        if ("SUCCESS".equals(payment.getStatus())) {
            throw new RuntimeException("Payment already processed");
        }
        if ("CANCELLED".equals(payment.getStatus())) {
            throw new RuntimeException("Cancelled payment cannot be verified");
        }

        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", req.getRazorpayOrderId());
        options.put("razorpay_payment_id", req.getRazorpayPaymentId());
        options.put("razorpay_signature", req.getRazorpaySignature());

        boolean isValid = Utils.verifyPaymentSignature(options, secret);

        if (!isValid) {
            throw new RuntimeException("Invalid signature");
        }

        BigDecimal amount = payment.getAmount();

        walletCommandService.createWallet(payment.getUserId());
        walletCommandService.topup(payment.getUserId(), amount, payment.getOrderId());

        payment.setPaymentId(req.getRazorpayPaymentId());
        payment.setStatus("SUCCESS");
        paymentRepo.save(payment);

        BigDecimal updatedBalance = walletQueryService.getBalance(payment.getUserId()).getBalance();

        kafkaProducer.send("payment-events", Map.of(
                "event", "PAYMENT_SUCCESS",
                "userId", payment.getUserId(),
                "amount", payment.getAmount(),
                "orderId", payment.getOrderId(),
                "balance", updatedBalance
        ));
    }

    @Override
    @Transactional
    public void cancelPayment(Long userId, String razorpayOrderId) {
        if (!StringUtils.hasText(razorpayOrderId)) {
            throw new RuntimeException("Razorpay order id is required");
        }

        Payment payment = paymentRepo.findById(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!userId.equals(payment.getUserId())) {
            throw new RuntimeException("Payment does not belong to authenticated user");
        }

        if ("SUCCESS".equals(payment.getStatus())) {
            throw new RuntimeException("Successful payment cannot be cancelled");
        }

        String referenceId = "CANCELLED_" + payment.getOrderId();
        Optional<Transaction> pendingTopup = transactionRepo.findByIdempotencyKey(payment.getOrderId());
        if (pendingTopup.isPresent()) {
            Transaction transaction = pendingTopup.get();
            if (transaction.getStatus() == Transaction.TxnStatus.CANCELLED) {
                payment.setStatus("CANCELLED");
                paymentRepo.save(payment);
                return;
            }

            payment.setStatus("CANCELLED");
            paymentRepo.save(payment);
            transaction.setStatus(Transaction.TxnStatus.CANCELLED);
            transaction.setReferenceId(referenceId);
            transaction.setDescription("Wallet top-up cancelled");
            transactionRepo.save(transaction);
            return;
        }

        payment.setStatus("CANCELLED");
        paymentRepo.save(payment);

        transactionRepo.save(Transaction.builder()
                .receiverId(payment.getUserId())
                .amount(payment.getAmount())
                .status(Transaction.TxnStatus.CANCELLED)
                .type(Transaction.TxnType.TOPUP)
                .referenceId(referenceId)
                .idempotencyKey(referenceId)
                .description("Wallet top-up cancelled")
                .build());

        kafkaProducer.send("payment-events", Map.of(
                "event", "PAYMENT_CANCELLED",
                "userId", payment.getUserId(),
                "amount", payment.getAmount(),
                "orderId", payment.getOrderId()
        ));
    }

    @Override
    @Transactional
    public void failPayment(Long userId, String razorpayOrderId, String reason) {
        updateUnsuccessfulPayment(
                userId,
                razorpayOrderId,
                "FAILED",
                Transaction.TxnStatus.FAILED,
                "FAILED_",
                StringUtils.hasText(reason) ? reason : "Wallet top-up failed",
                "PAYMENT_FAILED"
        );
    }

    private void updateUnsuccessfulPayment(
            Long userId,
            String razorpayOrderId,
            String paymentStatus,
            Transaction.TxnStatus transactionStatus,
            String referencePrefix,
            String description,
            String eventName) {
        if (!StringUtils.hasText(razorpayOrderId)) {
            throw new RuntimeException("Razorpay order id is required");
        }

        Payment payment = paymentRepo.findById(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!userId.equals(payment.getUserId())) {
            throw new RuntimeException("Payment does not belong to authenticated user");
        }

        if ("SUCCESS".equals(payment.getStatus())) {
            throw new RuntimeException("Successful payment cannot be marked unsuccessful");
        }

        String referenceId = referencePrefix + payment.getOrderId();
        Optional<Transaction> pendingTopup = transactionRepo.findByIdempotencyKey(payment.getOrderId());
        if (pendingTopup.isPresent()) {
            Transaction transaction = pendingTopup.get();
            payment.setStatus(paymentStatus);
            paymentRepo.save(payment);
            transaction.setStatus(transactionStatus);
            transaction.setReferenceId(referenceId);
            transaction.setDescription(description);
            transactionRepo.save(transaction);
        } else {
            payment.setStatus(paymentStatus);
            paymentRepo.save(payment);
            transactionRepo.save(Transaction.builder()
                    .receiverId(payment.getUserId())
                    .amount(payment.getAmount())
                    .status(transactionStatus)
                    .type(Transaction.TxnType.TOPUP)
                    .referenceId(referenceId)
                    .idempotencyKey(referenceId)
                    .description(description)
                    .build());
        }

        kafkaProducer.send("payment-events", Map.of(
                "event", eventName,
                "userId", payment.getUserId(),
                "amount", payment.getAmount(),
                "orderId", payment.getOrderId()
        ));
    }
}
