package com.loayaltyService.notification_service.client;

import com.loayaltyService.notification_service.config.FeignMessageConverterConfig;
import com.loayaltyService.notification_service.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", configuration = FeignMessageConverterConfig.class)
public interface UserClient {
    @GetMapping("/api/users/internal/users/{id}")
    UserDTO getProfile(@PathVariable Long id);
}
