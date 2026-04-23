package com.loayaltyService.notification_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loayaltyService.notification_service.client.UserClient;
import com.loayaltyService.notification_service.dto.UserDTO;
import com.loayaltyService.notification_service.service.impl.NotificationConsumerImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private UserClient userClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationConsumerImpl notificationConsumer;

    @Test
    void kycApprovedEventSendsHtmlEmail() throws Exception {
        when(objectMapper.readValue("event", Map.class))
                .thenReturn(Map.of("event", "KYC_APPROVED", "userId", 11));
        when(userClient.getProfile(11L)).thenReturn(UserDTO.builder().email("user@example.com").build());

        notificationConsumer.kycEvents("event");

        verify(emailService).sendHtml(eq("user@example.com"), eq("KYC Approved ✅"), anyString());
    }

    @Test
    void transferEventSendsToSenderAndReceiver() throws Exception {
        when(objectMapper.readValue("event", Map.class)).thenReturn(Map.of(
                "event", "TRANSFER_SUCCESS",
                "senderId", 10,
                "receiverId", 20,
                "amount", "250.00",
                "balance", "900.00",
                "reference", "TXN-123"
        ));
        when(userClient.getProfile(10L)).thenReturn(UserDTO.builder().email("sender@example.com").build());
        when(userClient.getProfile(20L)).thenReturn(UserDTO.builder().email("receiver@example.com").build());

        notificationConsumer.walletEvents("event");

        verify(emailService).sendHtml(eq("sender@example.com"), eq("Transfer Successful"), anyString());
        verify(emailService).sendHtml(eq("receiver@example.com"), eq("Money Received"), anyString());
    }

    @Test
    void rewardEventUsesDefaultValuesWhenMissingOptionalFields() throws Exception {
        when(objectMapper.readValue("event", Map.class))
                .thenReturn(Map.of("event", "POINTS_EARNED", "userId", 9));
        when(userClient.getProfile(9L)).thenReturn(UserDTO.builder().email("reward@example.com").build());

        notificationConsumer.rewardEvents("event");

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendHtml(eq("reward@example.com"), eq("Reward Points Earned"), htmlCaptor.capture());
        assertTrue(htmlCaptor.getValue().contains("₹0"));
        assertTrue(htmlCaptor.getValue().contains("N/A"));
    }

    @Test
    void eventWithoutTypeIsIgnored() throws Exception {
        when(objectMapper.readValue("event", Map.class)).thenReturn(Map.of("userId", 1));

        notificationConsumer.paymentEvents("event");

        verify(emailService, never()).sendHtml(anyString(), anyString(), anyString());
        verify(userClient, never()).getProfile(anyLong());
    }

    @Test
    void parsingFailureIsHandledGracefully() throws Exception {
        when(objectMapper.readValue("bad", Map.class)).thenThrow(new JsonProcessingException("bad json") { });

        notificationConsumer.walletEvents("bad");

        verify(emailService, never()).sendHtml(anyString(), anyString(), anyString());
    }
}
