package com.loyaltyService.user_service.dto;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfileResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String status;
    private String kycStatus;
    private Instant createdAt;
}
