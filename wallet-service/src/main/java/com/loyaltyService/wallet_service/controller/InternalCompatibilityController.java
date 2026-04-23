package com.loyaltyService.wallet_service.controller;

import com.loyaltyService.wallet_service.dto.ApiResponse;
import com.loyaltyService.wallet_service.service.WalletCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Backward-compatibility endpoints for older internal callers.
 * Preferred endpoint remains: POST /api/wallet/internal/create?userId=...
 */
@RestController
@RequiredArgsConstructor
public class InternalCompatibilityController {

    private final WalletCommandService walletCommandService;

    @PostMapping({ "/rewards/internal/create-account", "/api/rewards/internal/create-account" })
    public ResponseEntity<ApiResponse<Void>> createWalletCompat(@RequestParam Long userId) {
        walletCommandService.createWallet(userId);
        return ResponseEntity.ok(ApiResponse.ok("Wallet create accepted"));
    }
}

