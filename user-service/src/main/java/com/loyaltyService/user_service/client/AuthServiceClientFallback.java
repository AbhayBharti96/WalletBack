package com.loyaltyService.user_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthServiceClientFallback implements AuthServiceClient {

    @Override
    public void updateProfile(UpdateProfileRequest request) {
        log.error("auth-service unavailable - profile NOT synced for userId={}", request.userId());
    }

    @Override
    public void updateStatus(StatusUpdateRequest request) {
        log.error("auth-service unavailable - status NOT synced for userId={}, status={}",
                request.userId(), request.status());
    }

    @Override
    public void updateRole(RoleUpdateRequest request) {
        log.error("auth-service unavailable - role NOT synced for userId={}, role={}",
                request.userId(), request.role());
        throw new IllegalStateException("auth-service unavailable - role not synced");
    }
}
