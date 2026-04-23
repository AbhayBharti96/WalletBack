package com.loyaltyService.user_service.controller;

import com.loyaltyService.user_service.dto.AdminDashboardResponse;
import com.loyaltyService.user_service.dto.AdminUserResponse;
import com.loyaltyService.user_service.dto.ApiResponse;
import com.loyaltyService.user_service.entity.User;
import com.loyaltyService.user_service.exception.BadRequestException;
import com.loyaltyService.user_service.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin - Users", description = "Admin user management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private final AdminUserService adminUserService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard stats")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> dashboard(
            @RequestHeader("X-User-Role") String role) {
        requireAdmin(role);
        return ResponseEntity.ok(ApiResponse.ok("Dashboard fetched", adminUserService.getDashboard()));
    }

    @GetMapping("/users")
    @Operation(summary = "List all users with optional filters")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> listUsers(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = DEFAULT_SORT_FIELD) String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userRole) {
        requireAdmin(role);
        Pageable pageable = PageRequest.of(validatePage(page), validateSize(size), buildSort(sortBy, sortDir));
        User.UserStatus statusEnum = parseEnum(status, User.UserStatus.class, "status");
        User.Role roleEnum = parseEnum(userRole, User.Role.class, "userRole");
        return ResponseEntity.ok(ApiResponse.ok("Users fetched",
                adminUserService.listUsers(pageable, statusEnum, roleEnum)));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get full user details by ID")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUser(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long userId) {
        requireAdmin(role);
        return ResponseEntity.ok(ApiResponse.ok("User fetched", adminUserService.getUserById(userId)));
    }

    @GetMapping("/users/search")
    @Operation(summary = "Search users by name, email, or phone")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> searchByKeyword(
            @RequestHeader("X-User-Role") String role,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(role);
        Pageable pageable = PageRequest.of(validatePage(page), validateSize(size));
        return ResponseEntity.ok(ApiResponse.ok("Search results", adminUserService.searchUsers(q, pageable)));
    }

    @GetMapping("/users/search/email")
    @Operation(summary = "Find user by exact email")
    public ResponseEntity<ApiResponse<AdminUserResponse>> findByEmail(
            @RequestHeader("X-User-Role") String role,
            @RequestParam String email) {
        requireAdmin(role);
        return ResponseEntity.ok(ApiResponse.ok("User found", adminUserService.findByEmail(email)));
    }

    @GetMapping("/users/search/phone")
    @Operation(summary = "Find user by exact phone")
    public ResponseEntity<ApiResponse<AdminUserResponse>> findByPhone(
            @RequestHeader("X-User-Role") String role,
            @RequestParam String phone) {
        requireAdmin(role);
        return ResponseEntity.ok(ApiResponse.ok("User found", adminUserService.findByPhone(phone)));
    }

    @GetMapping("/users/search/date-range")
    @Operation(summary = "Find users registered between two dates")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> findByDateRange(
            @RequestHeader("X-User-Role") String role,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(role);
        if (from.isAfter(to)) {
            throw new BadRequestException("'from' date must be before or equal to 'to' date");
        }
        Pageable pageable = PageRequest.of(validatePage(page), validateSize(size), Sort.by(DEFAULT_SORT_FIELD).descending());
        return ResponseEntity.ok(ApiResponse.ok("Users fetched", adminUserService.findByDateRange(from, to, pageable)));
    }

    @GetMapping("/users/search/kyc-status")
    @Operation(summary = "Find users by KYC status")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> findByKycStatus(
            @RequestHeader("X-User-Role") String role,
            @RequestParam String kycStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(role);
        Pageable pageable = PageRequest.of(validatePage(page), validateSize(size));
        return ResponseEntity.ok(ApiResponse.ok("Users fetched", adminUserService.findByKycStatus(kycStatus, pageable)));
    }

    @PatchMapping("/users/{userId}/block")
    @Operation(summary = "Block a user")
    public ResponseEntity<ApiResponse<AdminUserResponse>> blockUser(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long userId) {
        requireAdmin(role);
        return ResponseEntity.ok(ApiResponse.ok("User blocked", adminUserService.setStatus(userId, User.UserStatus.BLOCKED)));
    }

    @PatchMapping("/users/{userId}/unblock")
    @Operation(summary = "Unblock a user")
    public ResponseEntity<ApiResponse<AdminUserResponse>> unblockUser(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long userId) {
        requireAdmin(role);
        return ResponseEntity.ok(ApiResponse.ok("User unblocked", adminUserService.setStatus(userId, User.UserStatus.ACTIVE)));
    }

    @PatchMapping("/users/{userId}/role")
    @Operation(summary = "Change user role")
    public ResponseEntity<ApiResponse<AdminUserResponse>> changeRole(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long userId,
            @RequestParam String newRole) {
        requireAdmin(role);
        User.Role targetRole = parseEnum(newRole, User.Role.class, "newRole");
        return ResponseEntity.ok(ApiResponse.ok("Role updated", adminUserService.changeRole(userId, targetRole)));
    }

    private void requireAdmin(String role) {
        if (!ADMIN_ROLE.equalsIgnoreCase(role)) {
            throw new BadRequestException("Access denied");
        }
    }

    private int validatePage(int page) {
        if (page < DEFAULT_PAGE) {
            throw new BadRequestException("page must be greater than or equal to 0");
        }
        return page;
    }

    private int validateSize(int size) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return size;
    }

    private Sort buildSort(String sortBy, String sortDir) {
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid sortDir: " + sortDir, exception);
        }
        return Sort.by(direction, sortBy);
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> enumType, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid " + fieldName + ": " + value, exception);
        }
    }
}
