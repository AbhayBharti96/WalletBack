package com.loyaltyService.user_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.loyaltyService.user_service.dto.KycStatusResponse;
import com.loyaltyService.user_service.entity.KycDetail;

@Mapper(componentModel = "spring")
public interface KycMapper {

    @Mapping(target = "kycId", source = "id")
    @Mapping(target = "userId", expression = "java(entity.getUser() != null ? entity.getUser().getId() : null)")
    @Mapping(target = "userName", expression = "java(entity.getUser() != null ? entity.getUser().getName() : null)")
    @Mapping(target = "userEmail", expression = "java(entity.getUser() != null ? entity.getUser().getEmail() : null)")
    @Mapping(target = "docType", expression = "java(entity.getDocType().name())")
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    @Mapping(target = "docFilePath", expression = "java(entity.getDocFilePath())")
    KycStatusResponse toResponse(KycDetail entity);
}