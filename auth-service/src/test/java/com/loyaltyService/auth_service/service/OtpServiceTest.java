package com.loyaltyService.auth_service.service;

import com.loyaltyService.auth_service.exception.AuthException;
import com.loyaltyService.auth_service.model.OtpStore;
import com.loyaltyService.auth_service.service.impl.OtpServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpServiceImpl(redisTemplate);
        ReflectionTestUtils.setField(otpService, "otpExpiryMinutes", 5);
        ReflectionTestUtils.setField(otpService, "otpLength", 6);
        ReflectionTestUtils.setField(otpService, "maxAttempts", 5);
        ReflectionTestUtils.setField(otpService, "rateLimitCapacity", 3);
        ReflectionTestUtils.setField(otpService, "rateLimitWindow", 10);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void generateAndSaveOtpForEmailStoresOtpAndTtl() {
        when(valueOperations.increment("otp:rate:LOGIN:user@example.com")).thenReturn(1L);

        String otp = otpService.generateAndSaveOtpForEmail("user@example.com", OtpStore.OtpType.LOGIN);

        assertNotNull(otp);
        assertEquals(6, otp.length());
        verify(redisTemplate).expire("otp:rate:LOGIN:user@example.com", 10, TimeUnit.MINUTES);
        verify(valueOperations).set(eq("otp:LOGIN:user@example.com"), eq(otp), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void generateAndSaveOtpForEmailThrowsWhenRateLimitExceeded() {
        when(valueOperations.increment("otp:rate:LOGIN:user@example.com")).thenReturn(4L);

        AuthException exception = assertThrows(AuthException.class,
                () -> otpService.generateAndSaveOtpForEmail("user@example.com", OtpStore.OtpType.LOGIN));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatus());
        verify(valueOperations, never()).set(anyString(), org.mockito.ArgumentMatchers.any(), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void verifyEmailOtpDeletesKeysOnSuccess() {
        when(valueOperations.get("otp:LOGIN:user@example.com")).thenReturn("123456");
        when(valueOperations.get("otp:attempts:LOGIN:user@example.com")).thenReturn(2);

        otpService.verifyEmailOtp("user@example.com", "123456", OtpStore.OtpType.LOGIN);

        verify(redisTemplate).delete("otp:LOGIN:user@example.com");
        verify(redisTemplate).delete("otp:attempts:LOGIN:user@example.com");
    }

    @Test
    void verifyEmailOtpThrowsWhenOtpMissing() {
        when(valueOperations.get("otp:LOGIN:user@example.com")).thenReturn(null);

        AuthException exception = assertThrows(AuthException.class,
                () -> otpService.verifyEmailOtp("user@example.com", "123456", OtpStore.OtpType.LOGIN));

        assertEquals(HttpStatus.GONE, exception.getStatus());
    }

    @Test
    void verifyEmailOtpThrowsWhenAttemptsExceeded() {
        when(valueOperations.get("otp:LOGIN:user@example.com")).thenReturn("123456");
        when(valueOperations.get("otp:attempts:LOGIN:user@example.com")).thenReturn(5);

        AuthException exception = assertThrows(AuthException.class,
                () -> otpService.verifyEmailOtp("user@example.com", "123456", OtpStore.OtpType.LOGIN));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatus());
        verify(redisTemplate).delete("otp:LOGIN:user@example.com");
    }

    @Test
    void verifyEmailOtpIncrementsAttemptsAndThrowsOnMismatch() {
        when(valueOperations.get("otp:LOGIN:user@example.com")).thenReturn("123456");
        when(valueOperations.get("otp:attempts:LOGIN:user@example.com")).thenReturn(null);
        when(valueOperations.increment("otp:attempts:LOGIN:user@example.com")).thenReturn(1L);

        AuthException exception = assertThrows(AuthException.class,
                () -> otpService.verifyEmailOtp("user@example.com", "000000", OtpStore.OtpType.LOGIN));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(redisTemplate).expire("otp:attempts:LOGIN:user@example.com", 5, TimeUnit.MINUTES);
    }

    @Test
    void getOtpExpiryMinutesReturnsConfiguredValue() {
        assertEquals(5, otpService.getOtpExpiryMinutes());
    }
}
