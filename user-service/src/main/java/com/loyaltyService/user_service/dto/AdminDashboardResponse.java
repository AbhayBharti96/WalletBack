package com.loyaltyService.user_service.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminDashboardResponse {
    // User counts
    private long totalUsers;
    private long activeUsers;
    private long blockedUsers;
    private long newUsersToday;
    private long newUsersThisWeek;
    private long newUsersThisMonth;

    // Role breakdown
    private long regularUsers;
    private long adminUsers;
    private long merchantUsers;

    // KYC stats
    private long kycPending;
    private long kycApproved;
    private long kycRejected;
    private long kycNotSubmitted;
    private long kycApprovedToday;
    private long kycRejectedToday;
}
