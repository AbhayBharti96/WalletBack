package com.loyaltyService.user_service.service.impl;

import com.loyaltyService.user_service.dto.ReceiverSearchResponse;
import com.loyaltyService.user_service.dto.UserProfileResponse;
import com.loyaltyService.user_service.entity.User;
import com.loyaltyService.user_service.exception.ResourceNotFoundException;
import com.loyaltyService.user_service.mapper.UserMapper;
import com.loyaltyService.user_service.repository.UserRepository;
import com.loyaltyService.user_service.service.KycStatusResolver;
import com.loyaltyService.user_service.service.UserQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CQRS — Query implementation.
 * Handles all read operations for User.
 * Results are cached in Redis under "user-profile::{userId}" for 15 minutes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepo;
    private final UserMapper userMapper;
    private final KycStatusResolver kycStatusResolver;

    @Override
    @Cacheable(value = "user-profile", key = "#userId")
    public UserProfileResponse getProfile(Long userId) {
        log.debug("Cache miss — loading user profile from DB for userId={}", userId);
        return buildUserProfile(userId);
    }

    @Override
    @Cacheable(value = "user-profile", key = "#userId")
    public UserProfileResponse getUserProfile(Long userId) {
        return buildUserProfile(userId);
    }

    @Override
    public List<ReceiverSearchResponse> searchReceivers(Long currentUserId, String query) {
        String normalized = query == null ? "" : query.trim();
        Long idQuery = parseLongOrNull(normalized);
        if (idQuery != null) {
            return userRepo.findByIdAndStatusAndDeletedAtIsNull(idQuery, User.UserStatus.ACTIVE)
                    .filter(receiver -> !receiver.getId().equals(currentUserId))
                    .map(receiver -> List.of(toReceiverSearchResponse(receiver)))
                    .orElseGet(List::of);
        }

        if (idQuery == null && normalized.length() < 2) {
            return List.of();
        }

        return userRepo.searchReceivers(currentUserId, normalized, idQuery, User.UserStatus.ACTIVE, PageRequest.of(0, 8))
                .stream()
                .map(this::toReceiverSearchResponse)
                .toList();
    }

    @Override
    @Cacheable(value = "user-status", key = "#userId")
    public String getUserStatus(Long userId) {
        log.debug("Cache miss — loading user status from DB for userId={}", userId);

        User user = findUser(userId);
        return user.getStatus() != null
                ? user.getStatus().name()
                : "ACTIVE";
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

    private Long parseLongOrNull(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private ReceiverSearchResponse toReceiverSearchResponse(User user) {
        return ReceiverSearchResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
