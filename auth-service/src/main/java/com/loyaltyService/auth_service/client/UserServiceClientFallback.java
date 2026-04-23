package com.loyaltyService.auth_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

@Slf4j
@Component
public class UserServiceClientFallback implements UserServiceClient {
    @Override
    public void createUser(CreateUserRequest request) {
        log.error("user-service unavailable — user profile NOT created for id={}", request.id());
    }
//    @Override
//    public String getUserStatus(@PathVariable Long userId){
//        log.error("user-service unavailable — cannot fetch status for userId={}", userId);
//        return "BLOCKED";
//    }
}
