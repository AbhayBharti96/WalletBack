package com.loyaltyService.user_service.dto;

import lombok.*;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminUserResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String status;
    private String role;
    private String kycStatus;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
