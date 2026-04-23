package com.loyaltyService.user_service.service;

import com.loyaltyService.user_service.dto.AdminDashboardResponse;
import com.loyaltyService.user_service.dto.AdminUserResponse;
import com.loyaltyService.user_service.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AdminUserService {

    AdminDashboardResponse getDashboard();

    Page<AdminUserResponse> listUsers(Pageable pageable, User.UserStatus status, User.Role role);

    AdminUserResponse getUserById(Long userId);

    Page<AdminUserResponse> searchUsers(String keyword, Pageable pageable);

    AdminUserResponse findByEmail(String email);

    AdminUserResponse findByPhone(String phone);

    Page<AdminUserResponse> findByDateRange(LocalDate from, LocalDate to, Pageable pageable);

    Page<AdminUserResponse> findByKycStatus(String kycStatus, Pageable pageable);

    AdminUserResponse setStatus(Long userId, User.UserStatus newStatus);

    AdminUserResponse changeRole(Long userId, User.Role newRole);
}
