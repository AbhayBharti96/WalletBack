package com.loyaltyService.auth_service.client;

import com.loyaltyService.auth_service.model.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {
    @PostMapping("/api/users/internal/create")
    void createUser(@RequestBody CreateUserRequest request);

//    @GetMapping("/api/users/internal/{userId}/status")
//    String getUserStatus(@PathVariable Long userId);

    record CreateUserRequest(Long id, String name, String email, String phone, User.Role role) {}
}
