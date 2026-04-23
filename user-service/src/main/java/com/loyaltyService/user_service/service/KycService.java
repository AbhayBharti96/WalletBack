package com.loyaltyService.user_service.service;

import com.loyaltyService.user_service.dto.KycStatusResponse;
import com.loyaltyService.user_service.entity.KycDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
public interface KycService {
    // User-facing
    KycStatusResponse submitKyc(Long userId, KycDetail.DocType docType,
                                String docNumber, MultipartFile docFile);
    KycStatusResponse getStatus(Long userId);

    // Admin-facing — by KYC record id
    Page<KycStatusResponse> getPendingKyc(Pageable pageable);
    KycStatusResponse approve(Long kycId, String adminEmail);
    KycStatusResponse reject(Long kycId, String reason, String adminEmail);

    // Admin-facing — by USER id (approve the latest pending KYC for a user)
    KycStatusResponse approveByUserId(Long userId, String adminEmail);
    KycStatusResponse rejectByUserId(Long userId, String reason, String adminEmail);

    // Admin dashboard counts
    long countPending();
    long countApprovedToday();
    long countRejectedToday();
}
