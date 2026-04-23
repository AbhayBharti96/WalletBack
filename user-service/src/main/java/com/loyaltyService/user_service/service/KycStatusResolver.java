package com.loyaltyService.user_service.service;

import com.loyaltyService.user_service.entity.KycDetail;
import com.loyaltyService.user_service.entity.User;
import com.loyaltyService.user_service.repository.KycRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KycStatusResolver {

    public static final String APPROVED = "APPROVED";
    public static final String NOT_SUBMITTED = "NOT_SUBMITTED";

    private final KycRepository kycRepo;

    public String resolve(User user) {
        if (user.getRole() == User.Role.ADMIN) {
            return APPROVED;
        }

        return kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(user.getId())
                .map(KycDetail::getStatus)
                .map(Enum::name)
                .orElse(NOT_SUBMITTED);
    }
}
