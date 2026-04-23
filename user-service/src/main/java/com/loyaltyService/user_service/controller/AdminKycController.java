package com.loyaltyService.user_service.controller;
import com.loyaltyService.user_service.dto.ApiResponse;
import com.loyaltyService.user_service.dto.KycStatusResponse;
import com.loyaltyService.user_service.entity.KycDetail;
import com.loyaltyService.user_service.service.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/admin/kyc")
@RequiredArgsConstructor
@Tag(name = "Admin KYC", description = "Admin endpoints for KYC review and approval")
@SecurityRequirement(name = "bearerAuth")
public class AdminKycController {

    private final KycService kycService;

    // ── List pending KYC ──────────────────────────────────────────────────────
    @GetMapping("/pending")
    @Operation(summary = "List all pending KYC submissions (paginated)")
    public ResponseEntity<?> pending(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (!isAdmin(role))
            return forbidden();

        Page<KycStatusResponse> result = kycService.getPendingKyc(
                PageRequest.of(page, size, Sort.by("submittedAt").ascending()));
        return ResponseEntity.ok(ApiResponse.ok("Pending KYC fetched", result));
    }

    // ── Approve by KYC record id ───────────────────────────────────────────────
    @PostMapping("/{kycId}/approve")
    @Operation(summary = "Approve KYC by KYC record ID")
    public ResponseEntity<?> approveByKycId(
            @PathVariable Long kycId,
            @RequestHeader("X-User-Role")  String role,
            @RequestHeader("X-User-Email") String adminEmail) {

        if (!isAdmin(role))
            return forbidden();

        KycStatusResponse result = kycService.approve(kycId, adminEmail);
        return ResponseEntity.ok(ApiResponse.ok("KYC approved", result));
    }

    // ── Approve by USER id ─────────────────────────────────────────────────────
    @PostMapping("/user/{userId}/approve")
    @Operation(summary = "Approve KYC by User ID (approves the latest pending KYC for that user)")
    public ResponseEntity<?> approveByUserId(
            @PathVariable Long userId,
            @RequestHeader("X-User-Role")  String role,
            @RequestHeader("X-User-Email") String adminEmail) {

        if (!isAdmin(role))
            return forbidden();

        KycStatusResponse result = kycService.approveByUserId(userId, adminEmail);
        return ResponseEntity.ok(ApiResponse.ok("KYC approved for userId: " + userId, result));
    }

    // ── Reject by KYC record id ────────────────────────────────────────────────
    @PostMapping("/{kycId}/reject")
    @Operation(summary = "Reject KYC by KYC record ID")
    public ResponseEntity<?> rejectByKycId(
            @PathVariable Long kycId,
            @RequestParam String reason,
            @RequestHeader("X-User-Role")  String role,
            @RequestHeader("X-User-Email") String adminEmail) {

        if (!isAdmin(role))
            return forbidden();

        KycStatusResponse result = kycService.reject(kycId, reason, adminEmail);
        return ResponseEntity.ok(ApiResponse.ok("KYC rejected", result));
    }

    // ── Reject by USER id ──────────────────────────────────────────────────────
    @PostMapping("/user/{userId}/reject")
    @Operation(summary = "Reject KYC by User ID")
    public ResponseEntity<?> rejectByUserId(
            @PathVariable Long userId,
            @RequestParam String reason,
            @RequestHeader("X-User-Role")  String role,
            @RequestHeader("X-User-Email") String adminEmail) {

        if (!isAdmin(role))
            return forbidden();

        KycStatusResponse result = kycService.rejectByUserId(userId, reason, adminEmail);
        return ResponseEntity.ok(ApiResponse.ok("KYC rejected for userId: " + userId, result));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private boolean isAdmin(String role) {
        return "ADMIN".equals(role) || "SUPPORT".equals(role);
    }

    private ResponseEntity<ApiResponse<Void>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .message("Access denied — ADMIN role required")
                        .build());
    }
}