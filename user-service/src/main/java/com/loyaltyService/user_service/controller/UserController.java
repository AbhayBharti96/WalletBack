package com.loyaltyService.user_service.controller;

import com.loyaltyService.user_service.dto.ApiResponse;
import com.loyaltyService.user_service.dto.ReceiverSearchResponse;
import com.loyaltyService.user_service.dto.UpdateUserRequest;
import com.loyaltyService.user_service.dto.UserProfileResponse;
import com.loyaltyService.user_service.entity.User;
import com.loyaltyService.user_service.service.UserCommandService;
import com.loyaltyService.user_service.service.UserQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CQRS Controller — GET endpoints use UserQueryService (Redis cached),
 * write endpoints use UserCommandService (cache-evicting).
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "User profile management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    // CQRS: Query side (cached reads)
    private final UserQueryService userQueryService;
    // CQRS: Command side (writes + cache eviction)
    private final UserCommandService userCommandService;

    @GetMapping("/profile")
    @Operation(summary = "Get my profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> profile(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Profile fetched", userQueryService.getProfile(userId)));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update my profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> update(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UpdateUserRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", userCommandService.updateProfile(userId, req)));
    }

    @GetMapping("/receivers/search")
    @Operation(summary = "Search transfer receivers")
    public ResponseEntity<ApiResponse<List<ReceiverSearchResponse>>> searchReceivers(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.ok("Receivers fetched", userQueryService.searchReceivers(userId, q)));
    }

    @PostMapping("/internal/create")
    public ResponseEntity<Void> createFromAuth(@RequestBody CreateUserRequest req) {
        userCommandService.createUser(req.id(), req.name(), req.email(), req.phone(), req.role());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/internal/users/{id}")
    public UserProfileResponse getUserInternal(@PathVariable Long id) {
        return userQueryService.getUserProfile(id);
    }

    record CreateUserRequest(Long id, String name, String email, String phone, User.Role role) {
    }

    @GetMapping("/internal/{userId}/status")
    public String getUserStatus(@PathVariable Long userId) {
        return userQueryService.getUserStatus(userId);
    }
}
