package com.loyaltyService.user_service.service.impl;

import com.loyaltyService.user_service.client.AuthServiceClient;
import com.loyaltyService.user_service.client.RewardServiceClient;
import com.loyaltyService.user_service.client.WalletServiceClient;
import com.loyaltyService.user_service.dto.UpdateUserRequest;
import com.loyaltyService.user_service.dto.UserProfileResponse;
import com.loyaltyService.user_service.entity.User;
import com.loyaltyService.user_service.exception.ResourceNotFoundException;
import com.loyaltyService.user_service.mapper.UserMapper;
import com.loyaltyService.user_service.repository.UserRepository;
import com.loyaltyService.user_service.service.KycStatusResolver;
import com.loyaltyService.user_service.service.UserCommandService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CQRS — Command implementation.
 * Handles all write (state-changing) operations for User.
 * Evicts the Redis cache on every mutation so that the query-side
 * always returns fresh data after a write.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepo;
    private final AuthServiceClient authServiceClient;
    private final WalletServiceClient walletServiceClient;
    private final RewardServiceClient rewardServiceClient;
    private final UserMapper userMapper;
    private final KycStatusResolver kycStatusResolver;

    @Override
    @Transactional
    @CacheEvict(value = "user-profile", key = "#id")
    public void createUser(Long id, String name, String email, String phone, User.Role role) {
        if (userRepo.existsById(id)) {
            User existing = findUser(id);
            existing.setName(name);
            existing.setEmail(email);
            existing.setPhone(phone);
            existing.setRole(role);
            existing.setStatus(User.UserStatus.ACTIVE);
            userRepo.save(existing);
            if (role == User.Role.ADMIN) {
                provisionAdminServiceAccounts(id);
            }
            log.info("User already exists and was synced: id={}", id);
            return;
        }
        User user = User.builder()
                .id(id).name(name).email(email)
                .phone(phone).role(role)
                .status(User.UserStatus.ACTIVE)
                .build();
        userRepo.save(user);
        if (role == User.Role.ADMIN) {
            provisionAdminServiceAccounts(id);
        }
        log.info("User created: id={}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "user-profile", key = "#userId")
    public UserProfileResponse updateProfile(Long userId, UpdateUserRequest req) {
        User user = findUser(userId);
        userMapper.updateUserFromDto(req, user);
        userRepo.save(user);

        // Sync with Auth Service
        authServiceClient.updateProfile(
                new AuthServiceClient.UpdateProfileRequest(userId, req.getName(), req.getPhone()));

        log.info("Profile updated and cache evicted for userId={}", userId);
        return buildUserProfile(userId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private UserProfileResponse buildUserProfile(Long userId) {
        User user = findUser(userId);
        return userMapper.toUserProfile(user, kycStatusResolver.resolve(user));
    }

    private User findUser(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private void provisionAdminServiceAccounts(Long userId) {
        try {
            walletServiceClient.createWallet(userId);
        } catch (Exception ex) {
            log.warn("Admin wallet provisioning failed for userId={}: {}", userId, ex.getMessage());
        }

        try {
            rewardServiceClient.createAccount(userId);
        } catch (Exception ex) {
            log.warn("Admin reward account provisioning failed for userId={}: {}", userId, ex.getMessage());
        }
    }
}
