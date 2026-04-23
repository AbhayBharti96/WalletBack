package com.loyaltyService.auth_service.config;

import com.loyaltyService.auth_service.client.UserServiceClient;
import com.loyaltyService.auth_service.model.User;
import com.loyaltyService.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserServiceClient userServiceClient;

    @Value("${app.bootstrap.admin.enabled:true}")
    private boolean adminBootstrapEnabled;

    @Value("${app.bootstrap.admin.full-name:Admin}")
    private String adminFullName;

    @Value("${app.bootstrap.admin.email:admin@vaultpay.local}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.phone:9999999999}")
    private String adminPhone;

    @Value("${app.bootstrap.admin.password:Admin@123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!adminBootstrapEnabled) {
            log.info("Admin bootstrap is disabled.");
            return;
        }

        String normalizedEmail = adminEmail == null ? "" : adminEmail.trim().toLowerCase(Locale.ROOT);
        String normalizedName = adminFullName == null ? "" : adminFullName.trim();
        String normalizedPhone = adminPhone == null ? "" : adminPhone.trim();

        if (normalizedEmail.isBlank() || normalizedName.isBlank() || normalizedPhone.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.warn("Admin bootstrap skipped due to missing config (name/email/phone/password).");
            return;
        }

        userRepository.findByEmail(normalizedEmail).ifPresentOrElse(existing -> {
            boolean changed = false;
            if (existing.getRole() != User.Role.ADMIN) {
                existing.setRole(User.Role.ADMIN);
                changed = true;
            }
            if (!existing.isActive()) {
                existing.setActive(true);
                changed = true;
            }
            if (changed) {
                User updated = userRepository.save(existing);
                syncToUserService(updated);
                log.info("Existing user promoted/updated as admin: email={}", normalizedEmail);
            } else {
                syncToUserService(existing);
                log.info("Admin already exists: email={}", normalizedEmail);
            }
        }, () -> {
            if (userRepository.existsByPhone(normalizedPhone)) {
                log.warn("Admin bootstrap skipped: phone {} is already used by another account.", normalizedPhone);
                return;
            }
            User admin = User.builder()
                    .fullName(normalizedName)
                    .email(normalizedEmail)
                    .phone(normalizedPhone)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(User.Role.ADMIN)
                    .isActive(true)
                    .build();
            User saved = userRepository.save(admin);
            syncToUserService(saved);
            log.info("Default admin created: email={}", normalizedEmail);
        });
    }

    private void syncToUserService(User user) {
        try {
            userServiceClient.createUser(new UserServiceClient.CreateUserRequest(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getRole()
            ));
        } catch (Exception ex) {
            log.warn("Admin created in auth-service but user-service sync failed for id={}: {}", user.getId(), ex.getMessage());
        }
    }
}
