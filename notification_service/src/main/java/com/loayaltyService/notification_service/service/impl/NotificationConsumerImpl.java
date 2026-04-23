package com.loayaltyService.notification_service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loayaltyService.notification_service.client.UserClient;
import com.loayaltyService.notification_service.service.EmailService;
import com.loayaltyService.notification_service.service.NotificationConsumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumerImpl implements NotificationConsumer {

    private final EmailService emailService;
    private final UserClient userClient;
    private final ObjectMapper objectMapper;

    @Override
    @KafkaListener(topics = "wallet-events", groupId = "notification-group")
    public void walletEvents(String event) {
        processEvent(event);
    }

    @Override
    @KafkaListener(topics = "payment-events", groupId = "notification-group")
    public void paymentEvents(String event) {
        processEvent(event);
    }

    @Override
    @KafkaListener(topics = "reward-events", groupId = "notification-group")
    public void rewardEvents(String event) {
        processEvent(event);
    }

    @Override
    @KafkaListener(topics = "kyc-events", groupId = "notification-group")
    public void kycEvents(String event) {
        processEvent(event);
    }

    @SuppressWarnings("unchecked")
    private void processEvent(String event) {
        log.info("Received Kafka event: {}", event);

        try {
            Map<String, Object> data = objectMapper.readValue(event, Map.class);
            String eventType = (String) data.get("event");

            if (eventType == null) {
                log.warn("Missing event type: {}", event);
                return;
            }

            switch (eventType) {

                // ================= KYC =================
                case "KYC_APPROVED":
                case "KYC_REJECTED": {
                    Long userId = Long.valueOf(data.get("userId").toString());
                    String email = userClient.getProfile(userId).getEmail();

                    String subject = eventType.equals("KYC_APPROVED")
                            ? "KYC Approved ✅"
                            : "KYC Rejected ❌";

                    String message = eventType.equals("KYC_APPROVED")
                            ? "Your KYC has been successfully approved."
                            : "Your KYC has been rejected. Reason: " + data.get("reason");

                    emailService.sendHtml(email, subject,
                            buildEmailHtml(subject, message, "-", "-", "KYC-" + userId, false));
                    break;
                }

                // ================= TRANSFER =================
                case "TRANSFER_SUCCESS": {
                    Long senderId = Long.valueOf(data.get("senderId").toString());
                    Long receiverId = Long.valueOf(data.get("receiverId").toString());

                    String senderEmail = userClient.getProfile(senderId).getEmail();
                    String receiverEmail = userClient.getProfile(receiverId).getEmail();

                    String amount = String.valueOf(data.get("amount"));
                    String senderBalance = String.valueOf(data.get("senderBalance"));
                    String receiverBalance = String.valueOf(data.get("receiverBalance"));
                    String reference = String.valueOf(data.getOrDefault("reference", "N/A"));

                    emailService.sendHtml(senderEmail, "Money Sent",
                            buildEmailHtml("Money Sent", "You sent money successfully.",
                                    amount, senderBalance, reference, false));

                    emailService.sendHtml(receiverEmail, "Money Received",
                            buildEmailHtml("Money Received", "You received money.",
                                    amount, receiverBalance, reference, false));
                    break;
                }

                // ================= COMMON EVENTS =================
                case "TOPUP_SUCCESS":
                case "WITHDRAW_SUCCESS":
                case "PAYMENT_SUCCESS":
                case "POINTS_EARNED":
                 {

                    Long userId = Long.valueOf(data.get("userId").toString());
                    String email = userClient.getProfile(userId).getEmail();

                    String amount = String.valueOf(data.getOrDefault("amount", "0"));
                    String balance = String.valueOf(data.getOrDefault("balance", "0"));
                    String reference = String.valueOf(data.getOrDefault("reference", "N/A"));

                    String subject = getSubject(eventType);
                    String message = getMessage(eventType);

                    // 🔥 IMPORTANT FIX
                    boolean isPoints = eventType.equals("POINTS_EARNED")
                            || eventType.equals("REDEEM_SUCCESS");

                    emailService.sendHtml(email, subject,
                            buildEmailHtml(subject, message, amount, balance, reference, isPoints));

                    break;
                }
                case "POINTS_REDEEMED":
                case "REDEEM_SUCCESS":{
                    Long userId = Long.valueOf(data.get("userId").toString());
                    String email = userClient.getProfile(userId).getEmail();

                    // ✅ Extract fields properly
                    String points = String.valueOf(data.getOrDefault("points", "0"));
                    String cash   = String.valueOf(data.getOrDefault("cash", "0"));
                    String balance = String.valueOf(data.getOrDefault("balance", "0"));
                    String reference = String.valueOf(data.getOrDefault("reference", "N/A"));

                    // ✅ Custom message for redeem
                    String subject;
                    String message;

                    if (eventType.equals("POINTS_REDEEMED")) {
                        subject = "Points Redeemed ✅";
                        message = "You redeemed " + points + " points and received ₹" + cash;
                    } else {
                        subject = getSubject(eventType);
                        message = getMessage(eventType);
                    }

                    // ✅ Always points display
                    boolean isPoints = true;

                    // 👇 Pass points as "amount"
                    emailService.sendHtml(
                            email,
                            subject,
                            buildEmailHtml(subject, message, points, balance, reference, isPoints)
                    );

                    break;
                }

                default:
                    log.warn("Unhandled event type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Error processing event", e);
        }
    }

    // ================= SUBJECT =================
    private String getSubject(String eventType) {
        return switch (eventType) {
            case "TOPUP_SUCCESS" -> "Wallet Top-up Successful";
            case "WITHDRAW_SUCCESS" -> "Withdrawal Successful";
            case "PAYMENT_SUCCESS" -> "Payment Successful";
            case "POINTS_EARNED" -> "Points Earned 🎉";
            case "REDEEM_SUCCESS" -> "Points Redeemed ✅";
            default -> "Notification";
        };
    }

    // ================= MESSAGE =================
    private String getMessage(String eventType) {
        return switch (eventType) {
            case "TOPUP_SUCCESS" -> "Money added to your wallet.";
            case "WITHDRAW_SUCCESS" -> "Money withdrawn from wallet.";
            case "PAYMENT_SUCCESS" -> "Payment completed successfully.";
            case "POINTS_EARNED" -> "You earned reward points.";
            case "REDEEM_SUCCESS" -> "You redeemed reward points.";
            default -> "Transaction update.";
        };
    }

    // ================= EMAIL TEMPLATE =================
    private String buildEmailHtml(String title, String message,
                                  String amount, String balance,
                                  String reference, boolean isPoints) {

        String symbol = isPoints ? "" : "₹";
        String unit   = isPoints ? " pts" : "";
        String label  = isPoints ? "Points" : "Amount";

        return "<!DOCTYPE html>" +
                "<html><body style='font-family:Arial;background:#f4f6f8;padding:20px;'>" +
                "<div style='max-width:600px;margin:auto;background:white;border-radius:10px;overflow:hidden;'>" +

                "<div style='background:#2c3e50;color:white;padding:20px;text-align:center;'>" +
                "<h2>Loyalty Wallet</h2></div>" +

                "<div style='padding:25px;'>" +
                "<h3>" + title + "</h3>" +
                "<p>" + message + "</p>" +

                "<p><b>" + label + ":</b> " + symbol + amount + unit + "</p>" +
                "<p><b>Reference:</b> " + reference + "</p>" +
                "<p><b>Balance:</b> " + symbol + balance + unit + "</p>" +

                "</div></div></body></html>";
    }
}