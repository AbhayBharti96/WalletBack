package com.loyaltyService.user_service.dto;
import jakarta.validation.constraints.Size;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateUserRequest {
    @Size(min = 2, max = 100) private String name;
    @Size(min = 10, max = 15) private String phone;
}
