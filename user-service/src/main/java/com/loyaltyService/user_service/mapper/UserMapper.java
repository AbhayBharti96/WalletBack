package com.loyaltyService.user_service.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.loyaltyService.user_service.dto.AdminUserResponse;
import com.loyaltyService.user_service.dto.UpdateUserRequest;
import com.loyaltyService.user_service.dto.UserProfileResponse;
import com.loyaltyService.user_service.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
	@Mapping(target = "status", expression = "java(user.getStatus().name())")
	@Mapping(target = "kycStatus", expression = "java(kycStatus != null ? kycStatus : \"NOT_SUBMITTED\")")
	UserProfileResponse toUserProfile(User user, String kycStatus);
	AdminUserResponse toAdminResponse(User user);
	
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromDto(UpdateUserRequest dto, @MappingTarget User user);
}
