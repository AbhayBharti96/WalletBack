package com.loayaltyService.notification_service.service;

import com.loayaltyService.notification_service.service.impl.EmailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Test
    void sendBuildsAndSendsSimpleMailMessage() {
        emailService.send("user@example.com", "Hello", "Body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals("user@example.com", captor.getValue().getTo()[0]);
        assertEquals("Hello", captor.getValue().getSubject());
        assertEquals("Body", captor.getValue().getText());
    }

    @Test
    void sendHtmlCreatesMimeMessageAndSendsIt() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendHtml("user@example.com", "Hello", "<b>Body</b>");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendHtmlSwallowsMailExceptions() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("boom"));

        emailService.sendHtml("user@example.com", "Hello", "<b>Body</b>");
    }
}
