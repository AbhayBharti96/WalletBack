package com.loyaltyService.user_service.service;

import com.loyaltyService.user_service.dto.UpdateUserRequest;
import com.loyaltyService.user_service.dto.UserProfileResponse;
import com.loyaltyService.user_service.entity.User;

/**
 * CQRS — Command side: all write operations for User.
 */
public interface UserCommandService {

    void createUser(Long id, String name, String email, String phone, User.Role role);

    UserProfileResponse updateProfile(Long userId, UpdateUserRequest req);
}
