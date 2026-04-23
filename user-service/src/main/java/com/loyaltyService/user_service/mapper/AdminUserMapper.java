package com.loyaltyService.user_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.loyaltyService.user_service.dto.AdminUserResponse;
import com.loyaltyService.user_service.entity.User;

@Mapper(componentModel = "spring")
public interface AdminUserMapper {

    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    @Mapping(target = "role", expression = "java(user.getRole().name())")
    AdminUserResponse toDto(User user);
}