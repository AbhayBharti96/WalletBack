package com.loyaltyService.user_service.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserSearchRequest {
    private String keyword;    // searches name, email, phone
    private String status;
    private String role;
    private String kycStatus;
    private String dateFrom;
    private String dateTo;
}
