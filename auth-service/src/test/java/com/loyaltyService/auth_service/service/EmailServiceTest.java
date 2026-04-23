package com.loyaltyService.auth_service.service;

import com.loyaltyService.auth_service.service.impl.EmailServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Test
    void sendOtpBuildsAndSendsMailMessage() {
        ReflectionTestUtils.setField(emailService, "mailSender", mailSender);

        emailService.sendOtp("user@example.com", "123456");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals("shivamkumar1352002@gmail.com", message.getFrom());
        assertEquals("user@example.com", message.getTo()[0]);
        assertEquals("Your OTP Code", message.getSubject());
        assertEquals("Your OTP is: 123456", message.getText());
    }
}
