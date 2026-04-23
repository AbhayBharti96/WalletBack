package com.loyaltyService.reward_service.controller;

import com.loyaltyService.reward_service.dto.*;
import com.loyaltyService.reward_service.entity.Redemption;
import com.loyaltyService.reward_service.entity.RewardItem;
import com.loyaltyService.reward_service.entity.RewardTransaction;
import com.loyaltyService.reward_service.service.RewardCommandService;
import com.loyaltyService.reward_service.service.RewardQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * CQRS Controller — GET endpoints use RewardQueryService (Redis cached),
 * write endpoints use RewardCommandService (cache-evicting).
 */
@Validated
@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
@Tag(name = "Rewards", description = "Points, tiers, catalog, redemption")
@SecurityRequirement(name = "bearerAuth")
public class RewardController {

    // CQRS: Query side (cached reads)
    private final RewardQueryService rewardQueryService;
    // CQRS: Command side (writes + cache eviction)
    private final RewardCommandService rewardCommandService;

    // ── Summary ───────────────────────────────────────────────────────────────
    @GetMapping("/summary")
    @Operation(summary = "Get reward summary (points, tier, next tier)")
    public ResponseEntity<ApiResponse<RewardSummaryDto>> summary(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Summary fetched", rewardQueryService.getSummary(userId)));
    }

    // ── Catalog ───────────────────────────────────────────────────────────────
    @GetMapping("/catalog")
    @Operation(summary = "Get reward catalog")
    public ResponseEntity<ApiResponse<List<RewardItem>>> catalog() {
        return ResponseEntity.ok(ApiResponse.ok("Catalog fetched", rewardQueryService.getCatalog()));
    }

    // ── Redeem catalog item ───────────────────────────────────────────────────
    @PostMapping("/redeem")
    @Operation(summary = "Redeem a catalog reward item (CASHBACK items credit wallet automatically)")
    public ResponseEntity<ApiResponse<Redemption>> redeem(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody RedeemRequest req) {
        Redemption r = rewardCommandService.redeemReward(userId, req.getRewardId());
        return ResponseEntity.ok(ApiResponse.ok("Redemption successful", r));
    }

    // ── Redeem points → wallet cash ───────────────────────────────────────────
    @PostMapping("/redeem-points")
    @Operation(summary = "Redeem points as wallet cash (1 point = ₹1). Daily cap applies.")
    public ResponseEntity<ApiResponse<Void>> redeemPoints(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam @Min(value = 1, message = "Points must be at least 1") Integer points) {
        rewardCommandService.redeemPoints(userId, points);
        return ResponseEntity.ok(ApiResponse.ok(
                "Successfully redeemed " + points + " points. ₹" + points + " credited to your wallet."));
    }
//    // ── Convert points → cash (backward compat) ───────────────────────────────
//    @PostMapping("/convert-to-cash")
//    @Operation(summary = "Convert points to wallet cash — alias for /redeem-points")
//    public ResponseEntity<ApiResponse<Void>> convertToCash(
//            @RequestHeader("X-User-Id") Long userId,
//            @RequestParam @Min(value = 1, message = "Points must be at least 1") Integer points) {
//        rewardCommandService.convertPointsToCash(userId, points);
//        return ResponseEntity.ok(ApiResponse.ok(
//                "Points converted successfully. ₹" + points + " credited to wallet."));
//    }

    // ── Transaction history ───────────────────────────────────────────────────
    @GetMapping("/transactions")
    public ResponseEntity<PageResponse<RewardTransaction>> getTransactions(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(
                rewardQueryService.getTransactions(userId, pageable)
        );
    }

    // ── Admin — add catalog item ──────────────────────────────────────────────
    @PostMapping("/catalog/add")
    @Operation(summary = "Admin — add reward item to catalog")
    public ResponseEntity<ApiResponse<RewardItem>> addCatalogItem(
            @RequestHeader("X-User-Role") String role,
            @RequestBody RewardItemRequest req) {
        if (!isAdmin(role)) {
            return forbiddenResponse();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Reward item added", rewardCommandService.addCatalogItem(req)));
    }

    // ── Admin — read/update/delete catalog ───────────────────────────────────
    @GetMapping("/admin/catalog")
    @Operation(summary = "Admin — read complete reward catalog")
    public ResponseEntity<ApiResponse<List<RewardItem>>> adminCatalog(
            @RequestHeader("X-User-Role") String role) {
        if (!isAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.<List<RewardItem>>builder()
                            .success(false)
                            .message("Access denied — ADMIN role required")
                            .build());
        }

        return ResponseEntity.ok(
                ApiResponse.ok("Admin catalog fetched", rewardQueryService.getCatalogForAdmin()));
    }

    @PutMapping("/admin/catalog/{rewardId}")
    @Operation(summary = "Admin — update reward catalog item")
    public ResponseEntity<ApiResponse<RewardItem>> updateCatalogItem(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long rewardId,
            @RequestBody RewardItemRequest req) {
        if (!isAdmin(role)) {
            return forbiddenResponse();
        }
        return ResponseEntity.ok(
                ApiResponse.ok("Reward item updated", rewardCommandService.updateCatalogItem(rewardId, req)));
    }

    @DeleteMapping("/admin/catalog/{rewardId}")
    @Operation(summary = "Admin — delete reward catalog item")
    public ResponseEntity<ApiResponse<Void>> deleteCatalogItem(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long rewardId) {
        if (!isAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Access denied — ADMIN role required")
                            .build());
        }
        rewardCommandService.deleteCatalogItem(rewardId);
        return ResponseEntity.ok(ApiResponse.ok("Reward item deleted"));
    }

    // ── Internal endpoints (service-to-service) ───────────────────────────────
    @PostMapping("/internal/create-account")
    @Operation(summary = "Internal — create reward account for new user")
    public ResponseEntity<Void> createAccount(@RequestParam Long userId) {
        rewardCommandService.createAccountIfNotExists(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/earn")
    @Operation(summary = "Internal — earn points from topup (called by wallet-service)")
    public ResponseEntity<Void> earnInternal(
            @RequestParam Long userId,
            @RequestParam java.math.BigDecimal amount) {
        rewardCommandService.earnPoints(userId, amount);
        return ResponseEntity.ok().build();
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equalsIgnoreCase(role);
    }

    private ResponseEntity<ApiResponse<RewardItem>> forbiddenResponse() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.<RewardItem>builder()
                        .success(false)
                        .message("Access denied — ADMIN role required")
                        .build());
    }
}
