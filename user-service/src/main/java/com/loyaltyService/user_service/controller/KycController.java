package com.loyaltyService.user_service.controller;

import com.loyaltyService.user_service.dto.ApiResponse;
import com.loyaltyService.user_service.dto.KycStatusResponse;
import com.loyaltyService.user_service.entity.KycDetail;
import com.loyaltyService.user_service.service.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
@RestController @RequestMapping("/api/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC", description = "KYC document submission and status")
@SecurityRequirement(name = "bearerAuth")
public class KycController {
    private final KycService kycService;
    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit KYC documents")
    public ResponseEntity<ApiResponse<KycStatusResponse>> submit(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam KycDetail.DocType docType,
            @RequestParam String docNumber,
            @RequestParam(required = false) MultipartFile docFile) {
        return ResponseEntity.ok(ApiResponse.ok("KYC submitted successfully",
            kycService.submitKyc(userId, docType, docNumber, docFile)));
    }
    @GetMapping("/status")
    @Operation(summary = "Check KYC status")
    public ResponseEntity<ApiResponse<KycStatusResponse>> status(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("KYC status fetched", kycService.getStatus(userId)));
    }
}
