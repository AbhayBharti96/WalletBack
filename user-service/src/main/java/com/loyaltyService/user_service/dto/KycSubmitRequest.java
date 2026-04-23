package com.loyaltyService.user_service.dto;

import com.loyaltyService.user_service.entity.KycDetail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KycSubmitRequest {
    @NotNull(message = "Document type is required") private KycDetail.DocType docType;
    @NotBlank(message = "Document number is required") private String docNumber;
}
