package com.loayaltyService.notification_service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loayaltyService.notification_service.client.UserClient;
import com.loayaltyService.notification_service.service.EmailService;
import com.loayaltyService.notification_service.service.NotificationConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
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
                case "KYC_APPROVED":
                case "KYC_REJECTED": {
                    Long userId = Long.valueOf(data.get("userId").toString());
                    String email = userClient.getProfile(userId).getEmail();

                    String subject = eventType.equals("KYC_APPROVED")
                            ? "KYC Approved"
                            : "KYC Rejected";

                    String message = eventType.equals("KYC_APPROVED")
                            ? "Your KYC has been successfully approved."
                            : "Your KYC has been rejected. Reason: " + data.get("reason");

                    emailService.sendHtml(email, subject,
                            buildEmailHtml(subject, message, "-", "-", "KYC-" + userId, false));
                    break;
                }

                case "TRANSFER_SUCCESS": {
                    Long senderId = Long.valueOf(data.get("senderId").toString());
                    Long receiverId = Long.valueOf(data.get("receiverId").toString());

                    String senderEmail = userClient.getProfile(senderId).getEmail();
                    String receiverEmail = userClient.getProfile(receiverId).getEmail();

                    String amount = String.valueOf(data.get("amount"));
                    String senderBalance = String.valueOf(data.get("senderBalance"));
                    String receiverBalance = String.valueOf(data.get("receiverBalance"));
                    String reference = String.valueOf(data.getOrDefault("reference", ""));

                    emailService.sendHtml(senderEmail, "Money Sent",
                            buildEmailHtml("Money Sent", "You sent money successfully.",
                                    amount, senderBalance, reference, false));

                    emailService.sendHtml(receiverEmail, "Money Received",
                            buildEmailHtml("Money Received", "You received money.",
                                    amount, receiverBalance, reference, false));
                    break;
                }

                case "TOPUP_SUCCESS":
                case "WITHDRAW_SUCCESS":
                case "PAYMENT_SUCCESS":
                case "POINTS_EARNED": {
                    Long userId = Long.valueOf(data.get("userId").toString());
                    String email = userClient.getProfile(userId).getEmail();

                    String amount = String.valueOf(data.getOrDefault("amount", "0"));
                    String balance = String.valueOf(data.getOrDefault("balance", "0"));
                    String reference = String.valueOf(data.getOrDefault("reference", ""));

                    String subject = getSubject(eventType);
                    String message = getMessage(eventType);
                    boolean isPoints = eventType.equals("POINTS_EARNED")
                            || eventType.equals("REDEEM_SUCCESS");

                    emailService.sendHtml(email, subject,
                            buildEmailHtml(subject, message, amount, balance, reference, isPoints));
                    break;
                }

                case "POINTS_REDEEMED":
                case "REDEEM_SUCCESS": {
                    Long userId = Long.valueOf(data.get("userId").toString());
                    String email = userClient.getProfile(userId).getEmail();

                    String points = String.valueOf(data.getOrDefault("points", "0"));
                    String cash = String.valueOf(data.getOrDefault("cash", "0"));
                    String balance = String.valueOf(data.getOrDefault("balance", "0"));
                    String reference = String.valueOf(data.getOrDefault("reference", ""));

                    String subject;
                    String message;

                    if (eventType.equals("POINTS_REDEEMED")) {
                        subject = "Points Redeemed";
                        message = "You redeemed " + points + " points and received Rs " + cash;
                    } else {
                        subject = getSubject(eventType);
                        message = getMessage(eventType);
                    }

                    emailService.sendHtml(
                            email,
                            subject,
                            buildEmailHtml(subject, message, points, balance, reference, true)
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

    private String getSubject(String eventType) {
        return switch (eventType) {
            case "TOPUP_SUCCESS" -> "Wallet Top-up Successful";
            case "WITHDRAW_SUCCESS" -> "Withdrawal Successful";
            case "PAYMENT_SUCCESS" -> "Payment Successful";
            case "POINTS_EARNED" -> "Points Earned";
            case "REDEEM_SUCCESS" -> "Points Redeemed";
            default -> "Notification";
        };
    }

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

    private String buildEmailHtml(String title, String message,
                                  String amount, String balance,
                                  String reference, boolean isPoints) {
        String amountValue = normalizeValue(amount);
        String balanceValue = normalizeValue(balance);
        String referenceValue = normalizeValue(reference);

        String accentColor = isPoints ? "#6d28d9" : "#0f766e";
        String badgeLabel = isPoints ? "Reward update" : "Wallet update";
        String primaryLabel = isPoints ? "Points earned" : "Transaction amount";
        String balanceLabel = isPoints ? "Updated points balance" : "Available wallet balance";
        String primaryDisplay = isPoints ? formatPoints(amountValue) : formatCurrency(amountValue);
        String balanceDisplay = isPoints ? formatPoints(balanceValue) : formatCurrency(balanceValue);

        List<String> detailRows = new ArrayList<>();
        if (primaryDisplay != null) {
            detailRows.add(buildDetailRow(primaryLabel, primaryDisplay));
        }
        if (balanceDisplay != null) {
            detailRows.add(buildDetailRow(balanceLabel, balanceDisplay));
        }
        if (referenceValue != null) {
            detailRows.add(buildDetailRow("Reference", referenceValue));
        }

        String summaryCards = buildSummaryCards(primaryLabel, primaryDisplay, balanceLabel, balanceDisplay);
        String detailsSection = detailRows.isEmpty()
                ? ""
                : "<div style='margin-top:24px;border:1px solid #e5e7eb;border-radius:14px;padding:18px 18px 6px;background:#ffffff;'>"
                + "<div style='font-size:14px;font-weight:700;color:#111827;margin-bottom:8px;'>Details</div>"
                + String.join("", detailRows)
                + "</div>";
        String nextStepText = isPoints
                ? "Use your wallet again to keep your rewards balance growing."
                : "Open your wallet activity to review this transaction and keep your balance on track.";

        return "<!DOCTYPE html>"
                + "<html><body style='margin:0;padding:0;background:#eef2f7;font-family:Arial,sans-serif;color:#111827;'>"
                + "<div style='padding:24px 12px;'>"
                + "<div style='max-width:640px;margin:0 auto;background:#ffffff;border-radius:20px;overflow:hidden;border:1px solid #dbe4ee;box-shadow:0 12px 36px rgba(15,23,42,0.08);'>"
                + "<div style='background:#1f2937;padding:28px 32px;'>"
                + "<div style='display:inline-block;padding:6px 12px;border-radius:999px;background:rgba(255,255,255,0.12);color:#e5e7eb;font-size:12px;font-weight:700;letter-spacing:0.3px;text-transform:uppercase;'>"
                + badgeLabel + "</div>"
                + "<h1 style='margin:16px 0 8px;font-size:28px;line-height:1.2;color:#ffffff;'>Loyalty Wallet</h1>"
                + "<p style='margin:0;font-size:15px;line-height:1.6;color:#cbd5e1;'>Your latest wallet activity is ready.</p>"
                + "</div>"
                + "<div style='padding:32px;'>"
                + "<h2 style='margin:0 0 10px;font-size:26px;line-height:1.3;color:#111827;'>" + title + "</h2>"
                + "<p style='margin:0 0 24px;font-size:15px;line-height:1.7;color:#4b5563;'>" + message + "</p>"
                + summaryCards
                + detailsSection
                + "<div style='margin-top:24px;padding:18px 20px;border-radius:14px;background:#f8fafc;border:1px solid #e2e8f0;'>"
                + "<div style='font-size:14px;font-weight:700;color:" + accentColor + ";margin-bottom:8px;'>What to do next</div>"
                + "<p style='margin:0;font-size:14px;line-height:1.6;color:#475569;'>" + nextStepText + "</p>"
                + "</div>"
                + "</div>"
                + "</div>"
                + "</div></body></html>";
    }

    private String buildSummaryCards(String primaryLabel, String primaryValue,
                                     String balanceLabel, String balanceValue) {
        List<String> cards = new ArrayList<>();
        if (primaryValue != null) {
            cards.add(buildMetricCard(primaryLabel, primaryValue));
        }
        if (balanceValue != null) {
            cards.add(buildMetricCard(balanceLabel, balanceValue));
        }

        if (cards.isEmpty()) {
            return "";
        }

        return "<div style='font-size:0;margin:0 -8px;'>" + String.join("", cards) + "</div>";
    }

    private String buildMetricCard(String label, String value) {
        return "<div style='display:inline-block;vertical-align:top;width:calc(50% - 16px);min-width:220px;margin:0 8px 16px;padding:18px;border-radius:16px;background:#f8fafc;border:1px solid #e2e8f0;box-sizing:border-box;'>"
                + "<div style='font-size:13px;line-height:1.5;color:#64748b;'>" + label + "</div>"
                + "<div style='margin-top:8px;font-size:28px;line-height:1.2;font-weight:700;color:#0f172a;'>" + value + "</div>"
                + "</div>";
    }

    private String buildDetailRow(String label, String value) {
        return "<div style='padding:0 0 12px;margin:0 0 12px;border-bottom:1px solid #e5e7eb;'>"
                + "<div style='font-size:12px;line-height:1.5;color:#64748b;text-transform:uppercase;letter-spacing:0.3px;'>" + label + "</div>"
                + "<div style='margin-top:4px;font-size:15px;line-height:1.6;color:#111827;font-weight:600;'>" + value + "</div>"
                + "</div>";
    }

    private String normalizeValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.equalsIgnoreCase("n/a") || normalized.equals("-")) {
            return null;
        }
        return normalized;
    }

    private String formatCurrency(String value) {
        BigDecimal amount = parseDecimal(value);
        if (amount == null) {
            return null;
        }
        return "Rs " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatPoints(String value) {
        BigDecimal amount = parseDecimal(value);
        if (amount == null) {
            return null;
        }
        return amount.stripTrailingZeros().toPlainString() + " pts";
    }

    private BigDecimal parseDecimal(String value) {
        String normalized = normalizeValue(value);
        if (normalized == null) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
