package com.loyaltyService.user_service.dto;
import lombok.*;
import java.time.Instant;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KycStatusResponse {
    private Long kycId;
    private Long userId;        // who submitted — needed for admin view
    private String userName;    // user's name for display in admin panel
    private String userEmail;   // user's email for display in admin panel
    private String docType;
    private String docNumber;
    private String docFilePath;
    private String status;
    private String rejectionReason;
    private Instant submittedAt;
    private Instant updatedAt;
}
