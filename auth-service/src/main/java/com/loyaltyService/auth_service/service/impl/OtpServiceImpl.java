//package com.loyaltyService.auth_service.service;
//
//import com.loyaltyService.auth_service.exception.AuthException;
//import com.loyaltyService.auth_service.model.OtpStore;
//import com.loyaltyService.auth_service.repository.OtpStoreRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpStatus;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.security.SecureRandom;
//import java.time.LocalDateTime;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class OtpService {
//
//    private final OtpStoreRepository otpStoreRepository;
//
//    @Value("${otp.expiry-minutes:5}")
//    private int otpExpiryMinutes;
//
//    @Value("${otp.length:6}")
//    private int otpLength;
//
//    @Value("${rate-limit.otp.capacity:3}")
//    private int otpRateLimitCapacity;
//
//    @Value("${rate-limit.otp.refill-minutes:10}")
//    private int otpRateLimitWindow;
//
//    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
//
//    @Transactional
//    public String generateAndSaveOtpForPhone(String phone, OtpStore.OtpType type) {
//        enforceRateLimitForPhone(phone, type);
//
//        String otp = generateOtp();
//
//        OtpStore otpStore = OtpStore.builder()
//                .phone(phone)
//                .otp(otp)
//                .otpType(type)
//                .expiryTime(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
//                .used(false)  // ✅ was .isUsed(false)
//                .build();
//
//        otpStoreRepository.save(otpStore);
//
//        log.info("OTP for phone {}: {} (expires in {} min)", maskPhone(phone), otp, otpExpiryMinutes);
//
//        return otp;
//    }
//
//    @Transactional
//    public String generateAndSaveOtpForEmail(String email, OtpStore.OtpType type) {
//        enforceRateLimitForEmail(email, type);
//
//        String otp = generateOtp();
//
//        OtpStore otpStore = OtpStore.builder()
//                .email(email)
//                .otp(otp)
//                .otpType(type)
//                .expiryTime(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
//                .used(false)  // ✅ was .isUsed(false)
//                .build();
//
//        otpStoreRepository.save(otpStore);
//
//        log.info("OTP for email {}: {} (expires in {} min)", maskEmail(email), otp, otpExpiryMinutes);
//
//        return otp;
//    }
//
//    @Transactional
//    public void verifyPhoneOtp(String phone, String otp, OtpStore.OtpType type) {
//        OtpStore otpStore = otpStoreRepository
//                .findTopByPhoneAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(phone, type)
//                .orElseThrow(() -> new AuthException("No active OTP found for this phone number", HttpStatus.BAD_REQUEST));
//
//        validateOtp(otpStore, otp);
//
//        otpStore.setUsed(true);
//        otpStoreRepository.save(otpStore);
//    }
//
//    @Transactional
//    public void verifyEmailOtp(String email, String otp, OtpStore.OtpType type) {
//        OtpStore otpStore = otpStoreRepository
//        		.findTopByEmailAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(email, type)
//                .orElseThrow(() -> new AuthException("No active OTP found for this email", HttpStatus.BAD_REQUEST));
//
//        validateOtp(otpStore, otp);
//
//        otpStore.setUsed(true);
//        otpStoreRepository.save(otpStore);
//    }
//
//    private void validateOtp(OtpStore otpStore, String providedOtp) {
//        if (otpStore.isUsed()) {
//            throw new AuthException("OTP has already been used.", HttpStatus.BAD_REQUEST);
//        }
//
//        if (otpStore.isExpired()) {
//            throw new AuthException("OTP has expired. Please request a new one.", HttpStatus.GONE);
//        }
//
//        otpStore.setAttemptCount(otpStore.getAttemptCount() + 1);
//        otpStoreRepository.save(otpStore);
//
//        if (!otpStore.getOtp().equals(providedOtp)) {
//            throw new AuthException("Invalid OTP. Please try again.", HttpStatus.BAD_REQUEST);
//        }
//    }
//
//    private void enforceRateLimitForPhone(String phone, OtpStore.OtpType type) {
//        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(otpRateLimitWindow);
//        long recentCount = otpStoreRepository.countRecentOtpsByPhone(phone, type, windowStart);
//
//        if (recentCount >= otpRateLimitCapacity) {
//            throw new AuthException(
//                    String.format("Too many OTP requests. Please try again after %d minutes.", otpRateLimitWindow),
//                    HttpStatus.TOO_MANY_REQUESTS
//            );
//        }
//    }
//
//    private void enforceRateLimitForEmail(String email, OtpStore.OtpType type) {
//        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(otpRateLimitWindow);
//        long recentCount = otpStoreRepository.countRecentOtpsByEmail(email, type, windowStart);
//
//        if (recentCount >= otpRateLimitCapacity) {
//            throw new AuthException(
//                    String.format("Too many OTP requests. Please try again after %d minutes.", otpRateLimitWindow),
//                    HttpStatus.TOO_MANY_REQUESTS
//            );
//        }
//    }
//
//    private String generateOtp() {
//        int max = (int) Math.pow(10, otpLength);
//        int min = (int) Math.pow(10, otpLength - 1);
//        int otp = min + SECURE_RANDOM.nextInt(max - min);
//        return String.valueOf(otp);
//    }
//
//    public int getOtpExpiryMinutes() {
//        return otpExpiryMinutes;
//    }
//
//    @Scheduled(fixedDelayString = "PT30M")
//    @Transactional
//    public void cleanupExpiredOtps() {
//        otpStoreRepository.deleteExpiredOtps(LocalDateTime.now());
//        log.info("Expired OTPs cleaned up");
//    }
//
//    private String maskPhone(String phone) {
//        if (phone == null || phone.length() < 4) return "****";
//        return "****" + phone.substring(phone.length() - 4);
//    }
//
//    private String maskEmail(String email) {
//        if (email == null || !email.contains("@")) return "****";
//        String[] parts = email.split("@");
//        String name = parts[0];
//        String domain = parts[1];
//        String masked = name.length() > 2
//                ? name.charAt(0) + "****" + name.charAt(name.length() - 1)
//                : "****";
//        return masked + "@" + domain;
//    }
//}

package com.loyaltyService.auth_service.service.impl;

import com.loyaltyService.auth_service.exception.AuthException;
import com.loyaltyService.auth_service.model.OtpStore;
import com.loyaltyService.auth_service.service.OtpService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${otp.length:6}")
    private int otpLength;

    @Value("${otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${otp.rate-limit.capacity:3}")
    private int rateLimitCapacity;

    @Value("${otp.rate-limit.window-minutes:10}")
    private int rateLimitWindow;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // =========================
    // GENERATE OTP
    // =========================
    @Override
    public String generateAndSaveOtpForEmail(String email, OtpStore.OtpType type) {

        enforceRateLimit(email, type);

        String otp = generateOtp();

        String otpKey = buildOtpKey(email, type);

        redisTemplate.opsForValue().set(
                otpKey,
                otp,
                otpExpiryMinutes,
                TimeUnit.MINUTES
        );

        log.info("OTP generated for {}", email);

        return otp;
    }

    // =========================
    // VERIFY OTP
    // =========================
    @Override
    public void verifyEmailOtp(String email, String otp, OtpStore.OtpType type) {

        String otpKey = buildOtpKey(email, type);
        String attemptsKey = buildAttemptsKey(email, type);

        Object storedOtpValue = redisTemplate.opsForValue().get(otpKey);
        String storedOtp = storedOtpValue == null ? null : String.valueOf(storedOtpValue);

        if (storedOtp == null) {
            throw new AuthException("OTP expired or not found", HttpStatus.GONE);
        }

        Object attemptsValue = redisTemplate.opsForValue().get(attemptsKey);
        Long attempts = toLongValue(attemptsValue);

        if (attempts != null && attempts >= maxAttempts) {
            redisTemplate.delete(otpKey);
            throw new AuthException("Too many failed attempts. OTP blocked.", HttpStatus.TOO_MANY_REQUESTS);
        }

        if (!storedOtp.equals(otp)) {

            Long newAttempts = redisTemplate.opsForValue().increment(attemptsKey);

            if (newAttempts == 1) {
                redisTemplate.expire(attemptsKey, otpExpiryMinutes, TimeUnit.MINUTES);
            }

            throw new AuthException("Invalid OTP", HttpStatus.BAD_REQUEST);
        }

        // SUCCESS → cleanup
        redisTemplate.delete(otpKey);
        redisTemplate.delete(attemptsKey);

        log.info("OTP verified successfully for {}", email);
    }

    // =========================
    // RATE LIMITING
    // =========================
    private void enforceRateLimit(String email, OtpStore.OtpType type) {

        String key = "otp:rate:" + type + ":" + email;

        Long count = redisTemplate.opsForValue().increment(key);

        if (count == 1) {
            redisTemplate.expire(key, rateLimitWindow, TimeUnit.MINUTES);
        }

        if (count > rateLimitCapacity) {
            throw new AuthException(
                    "Too many OTP requests. Try again later.",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }
    }

    // =========================
    // UTILS
    // =========================
    private String generateOtp() {
        int max = (int) Math.pow(10, otpLength);
        int min = (int) Math.pow(10, otpLength - 1);
        return String.valueOf(min + SECURE_RANDOM.nextInt(max - min));
    }

    private String buildOtpKey(String email, OtpStore.OtpType type) {
        return "otp:" + type + ":" + email;
    }

    private String buildAttemptsKey(String email, OtpStore.OtpType type) {
        return "otp:attempts:" + type + ":" + email;
    }

    private Long toLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            log.warn("Ignoring non-numeric Redis value for OTP attempts: {}", value);
            return null;
        }
    }

    @Override
    public int getOtpExpiryMinutes() {
        return otpExpiryMinutes;
    }
}
