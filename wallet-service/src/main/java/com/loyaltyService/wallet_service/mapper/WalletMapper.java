package com.loyaltyService.wallet_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.loyaltyService.wallet_service.dto.WalletBalanceResponse;
import com.loyaltyService.wallet_service.entity.WalletAccount;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    @Mapping(target = "status", expression = "java(account.getStatus().name())")
    @Mapping(target = "userId", source = "userId")
    WalletBalanceResponse toResponse(WalletAccount account, Long userId);
}