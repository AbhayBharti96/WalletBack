package com.loyaltyService.user_service.service.impl;

import com.loyaltyService.user_service.dto.UserProfileResponse;
import com.loyaltyService.user_service.entity.KycDetail;
import com.loyaltyService.user_service.entity.User;
import com.loyaltyService.user_service.exception.ResourceNotFoundException;
import com.loyaltyService.user_service.mapper.UserMapper;
import com.loyaltyService.user_service.repository.KycRepository;
import com.loyaltyService.user_service.repository.UserRepository;
import com.loyaltyService.user_service.service.KycStatusResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserQueryServiceImplTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private KycRepository kycRepo;

    @Mock
    private UserMapper userMapper;

    @Mock
    private KycStatusResolver kycStatusResolver;

    @InjectMocks
    private UserQueryServiceImpl userQueryService;

    private User testUser;
    private UserProfileResponse testResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).name("Test User").role(User.Role.USER).build();
        testResponse = new UserProfileResponse();
        testResponse.setId(1L);
        testResponse.setName("Test User");
        testResponse.setKycStatus("APPROVED");
    }

    @Test
    void testGetProfile_Success() {
        // Arrange
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        
        KycDetail kyc = KycDetail.builder()
                .status(KycDetail.KycStatus.APPROVED)
                .build();
        when(kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.of(kyc));
        when(kycStatusResolver.resolve(testUser)).thenReturn("APPROVED");
        
        when(userMapper.toUserProfile(testUser, "APPROVED")).thenReturn(testResponse);

        // Act
        UserProfileResponse res = userQueryService.getProfile(1L);

        // Assert
        assertNotNull(res);
        assertEquals("Test User", res.getName());
        assertEquals("APPROVED", res.getKycStatus());
    }

    @Test
    void testGetProfile_UserNotFound() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userQueryService.getProfile(99L));
    }

    @Test
    void testGetProfile_NoKyc() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.empty());
        when(kycStatusResolver.resolve(testUser)).thenReturn("NOT_SUBMITTED");
        when(userMapper.toUserProfile(testUser, "NOT_SUBMITTED")).thenReturn(testResponse);

        UserProfileResponse res = userQueryService.getProfile(1L);
        
        assertNotNull(res);
        verify(userMapper, times(1)).toUserProfile(testUser, "NOT_SUBMITTED");
    }

    @Test
    void testGetUserProfile_Success() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.empty());
        when(kycStatusResolver.resolve(testUser)).thenReturn("NOT_SUBMITTED");
        when(userMapper.toUserProfile(testUser, "NOT_SUBMITTED")).thenReturn(testResponse);

        UserProfileResponse res = userQueryService.getUserProfile(1L);
        
        assertNotNull(res);
    }
}
