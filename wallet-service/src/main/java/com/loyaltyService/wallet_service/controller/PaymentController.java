package com.loyaltyService.wallet_service.controller;

import com.loyaltyService.wallet_service.dto.PaymentCancelRequest;
import com.loyaltyService.wallet_service.dto.PaymentFailRequest;
import com.loyaltyService.wallet_service.dto.PaymentVerifyRequest;
import com.loyaltyService.wallet_service.service.RazorpayService;
import com.razorpay.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

        private final RazorpayService razorpayService;

        @PostMapping("/create-order")
        public ResponseEntity<?> createOrder(
                        @RequestHeader("X-User-Id") Long userId,
                        @RequestParam BigDecimal amount) throws Exception {

                Order order = razorpayService.createOrder(userId, amount);
                return ResponseEntity.ok(Map.of(
                                "orderId", order.get("id"),
                                "amount", order.get("amount"),
                                "currency", order.get("currency")));
        }

        @PostMapping("/verify")
        public ResponseEntity<?> verify(
                @RequestHeader("X-User-Id") Long userId,
                @RequestBody PaymentVerifyRequest request) {

                try {
                        razorpayService.verifyPayment(userId, request);
                        return ResponseEntity.ok("Payment verified & wallet credited");
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(e.getMessage());
                }
        }

        @PostMapping("/cancel")
        public ResponseEntity<?> cancel(
                @RequestHeader("X-User-Id") Long userId,
                @RequestBody PaymentCancelRequest request) {

                try {
                        razorpayService.cancelPayment(userId, request.getRazorpayOrderId());
                        return ResponseEntity.ok("Payment cancelled & transaction recorded");
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(e.getMessage());
                }
        }

        @PostMapping("/fail")
        public ResponseEntity<?> fail(
                @RequestHeader("X-User-Id") Long userId,
                @RequestBody PaymentFailRequest request) {

                try {
                        razorpayService.failPayment(userId, request.getRazorpayOrderId(), request.getReason());
                        return ResponseEntity.ok("Payment failed & transaction recorded");
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(e.getMessage());
                }
        }
}
