package com.loyaltyService.user_service.service.impl;

import com.loyaltyService.user_service.dto.AdminDashboardResponse;
import com.loyaltyService.user_service.dto.AdminUserResponse;
import com.loyaltyService.user_service.client.AuthServiceClient;
import com.loyaltyService.user_service.client.RewardServiceClient;
import com.loyaltyService.user_service.client.WalletServiceClient;
import com.loyaltyService.user_service.entity.KycDetail;
import com.loyaltyService.user_service.entity.User;
import com.loyaltyService.user_service.exception.ResourceNotFoundException;
import com.loyaltyService.user_service.mapper.AdminUserMapper;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.loyaltyService.user_service.entity.KycDetail.KycStatus.APPROVED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private KycRepository kycRepo;

    @Mock
    private AdminUserMapper adminUserMapper;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private WalletServiceClient walletServiceClient;

    @Mock
    private RewardServiceClient rewardServiceClient;

    @Mock
    private KycStatusResolver kycStatusResolver;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    @BeforeEach
    void setUp() {
        lenient().when(adminUserMapper.toDto(any(User.class))).thenAnswer(invocation -> toResponse(invocation.getArgument(0)));
        lenient().when(kycStatusResolver.resolve(any(User.class))).thenReturn("NOT_SUBMITTED");
    }

    @Test
    void getDashboardAggregatesRepositoryCounts() {
        when(userRepo.count()).thenReturn(10L);
        when(userRepo.countByStatus(User.UserStatus.ACTIVE)).thenReturn(7L);
        when(userRepo.countByStatus(User.UserStatus.BLOCKED)).thenReturn(3L);
        when(userRepo.countByCreatedAtAfter(any())).thenReturn(2L, 4L, 6L);
        when(userRepo.countByRole(User.Role.USER)).thenReturn(8L);
        when(userRepo.countByRole(User.Role.ADMIN)).thenReturn(1L);
        when(userRepo.countByRole(User.Role.MERCHANT)).thenReturn(1L);
        when(kycRepo.countByStatus(KycDetail.KycStatus.PENDING)).thenReturn(2L);
        when(kycRepo.countByStatus(APPROVED)).thenReturn(5L);
        when(kycRepo.countByStatus(KycDetail.KycStatus.REJECTED)).thenReturn(1L);
        when(kycRepo.countByStatusSince(any(), any())).thenReturn(3L, 1L);

        AdminDashboardResponse response = adminUserService.getDashboard();

        assertEquals(10L, response.getTotalUsers());
        assertEquals(7L, response.getActiveUsers());
        assertEquals(2L, response.getKycPending());
        assertEquals(2L, response.getKycNotSubmitted());
        assertEquals(3L, response.getKycApprovedToday());
        assertEquals(1L, response.getKycRejectedToday());
    }

    @Test
    void listUsersUsesCombinedFilterWhenStatusAndRolePresent() {
        User user = user(1L);
        KycDetail kyc = latestKyc(user, APPROVED);
        when(userRepo.findByStatusAndRole(User.UserStatus.ACTIVE, User.Role.USER, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1));
        when(kycStatusResolver.resolve(user)).thenReturn("APPROVED");

        var page = adminUserService.listUsers(PageRequest.of(0, 10), User.UserStatus.ACTIVE, User.Role.USER);

        assertEquals(1, page.getTotalElements());
        assertEquals("APPROVED", page.getContent().getFirst().getKycStatus());
    }

    @Test
    void listUsersUsesStatusFilterOnly() {
        User user = user(11L);
        when(userRepo.findByStatus(User.UserStatus.BLOCKED, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1));
        when(kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(11L)).thenReturn(Optional.empty());

        var page = adminUserService.listUsers(PageRequest.of(0, 10), User.UserStatus.BLOCKED, null);

        assertEquals(1, page.getTotalElements());
        verify(userRepo).findByStatus(User.UserStatus.BLOCKED, PageRequest.of(0, 10));
    }

    @Test
    void listUsersUsesRoleFilterOnly() {
        User user = user(12L);
        when(userRepo.findByRole(User.Role.MERCHANT, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1));
        when(kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(12L)).thenReturn(Optional.empty());

        var page = adminUserService.listUsers(PageRequest.of(0, 10), null, User.Role.MERCHANT);

        assertEquals(1, page.getTotalElements());
        verify(userRepo).findByRole(User.Role.MERCHANT, PageRequest.of(0, 10));
    }

    @Test
    void listUsersUsesFindAllWhenNoFiltersProvided() {
        User user = user(13L);
        when(userRepo.findAll(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1));
        when(kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(13L)).thenReturn(Optional.empty());

        var page = adminUserService.listUsers(PageRequest.of(0, 10), null, null);

        assertEquals(1, page.getTotalElements());
        verify(userRepo).findAll(PageRequest.of(0, 10));
    }

    @Test
    void getUserByIdThrowsWhenUserMissing() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminUserService.getUserById(99L));
    }

    @Test
    void getUserByIdReturnsMappedUser() {
        User user = user(14L);
        when(userRepo.findById(14L)).thenReturn(Optional.of(user));
        when(kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(14L)).thenReturn(Optional.empty());

        AdminUserResponse response = adminUserService.getUserById(14L);

        assertEquals(14L, response.getId());
        assertEquals("NOT_SUBMITTED", response.getKycStatus());
    }

    @Test
    void searchUsersDelegatesToRepository() {
        User user = user(15L);
        when(userRepo.searchByKeyword("admin", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1));
        when(kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(15L)).thenReturn(Optional.empty());

        var page = adminUserService.searchUsers("admin", PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        verify(userRepo).searchByKeyword("admin", PageRequest.of(0, 10));
    }

    @Test
    void findByEmailReturnsMappedUser() {
        User user = user(2L);
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(2L)).thenReturn(Optional.empty());

        AdminUserResponse response = adminUserService.findByEmail("test@example.com");

        assertEquals(2L, response.getId());
        assertEquals("NOT_SUBMITTED", response.getKycStatus());
    }

    @Test
    void findByEmailThrowsWhenUserMissing() {
        when(userRepo.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminUserService.findByEmail("missing@example.com"));
    }

    @Test
    void findByPhoneReturnsMappedUser() {
        User user = user(16L);
        when(userRepo.findByPhone("9876543210")).thenReturn(Optional.of(user));
        when(kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(16L)).thenReturn(Optional.empty());

        AdminUserResponse response = adminUserService.findByPhone("9876543210");

        assertEquals(16L, response.getId());
    }

    @Test
    void findByPhoneThrowsWhenUserMissing() {
        when(userRepo.findByPhone("000")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminUserService.findByPhone("000"));
    }

    @Test
    void findByDateRangeDelegatesToRepository() {
        User user = user(3L);
        when(userRepo.findByCreatedAtBetween(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 5), 1));
        when(kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(3L)).thenReturn(Optional.empty());

        var page = adminUserService.findByDateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), PageRequest.of(0, 5));

        assertEquals(1, page.getTotalElements());
        verify(userRepo).findByCreatedAtBetween(any(), any(), any());
    }

    @Test
    void findByKycStatusUppercasesStatusBeforeDelegating() {
        User user = user(17L);
        KycDetail kyc = latestKyc(user, APPROVED);
        when(userRepo.findByLatestKycStatus(eq(APPROVED), eq(PageRequest.of(0, 10))))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1));
        when(kycStatusResolver.resolve(user)).thenReturn("APPROVED");

        var page = adminUserService.findByKycStatus("approved", PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("APPROVED", page.getContent().getFirst().getKycStatus());
        verify(userRepo).findByLatestKycStatus(APPROVED, PageRequest.of(0, 10));
    }

    @Test
    void setStatusUpdatesAndPersistsUser() {
        User user = user(4L);
        when(userRepo.findById(4L)).thenReturn(Optional.of(user));
        when(kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(4L)).thenReturn(Optional.empty());

        AdminUserResponse response = adminUserService.setStatus(4L, User.UserStatus.BLOCKED);

        assertEquals("BLOCKED", response.getStatus());
        verify(userRepo).save(user);
    }

    @Test
    void setStatusThrowsWhenUserMissing() {
        when(userRepo.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminUserService.setStatus(404L, User.UserStatus.BLOCKED));
    }

    @Test
    void changeRoleUpdatesAndPersistsUser() {
        User user = user(5L);
        when(userRepo.findById(5L)).thenReturn(Optional.of(user));
        when(kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(5L)).thenReturn(Optional.empty());

        AdminUserResponse response = adminUserService.changeRole(5L, User.Role.ADMIN);

        assertEquals("ADMIN", response.getRole());
        verify(userRepo).save(user);
        verify(authServiceClient).updateRole(new AuthServiceClient.RoleUpdateRequest(5L, "ADMIN"));
        verify(walletServiceClient).createWallet(5L);
        verify(rewardServiceClient).createAccount(5L);
    }

    @Test
    void changeRoleThrowsWhenUserMissing() {
        when(userRepo.findById(405L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminUserService.changeRole(405L, User.Role.ADMIN));
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .name("Admin Test")
                .email("test@example.com")
                .phone("9876543210")
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-02T00:00:00Z"))
                .build();
    }

    private KycDetail latestKyc(User user, KycDetail.KycStatus status) {
        return KycDetail.builder()
                .id(10L)
                .user(user)
                .docType(KycDetail.DocType.AADHAAR)
                .docNumber("123412341234")
                .status(status)
                .submittedAt(Instant.parse("2026-01-02T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-02T01:00:00Z"))
                .build();
    }

    private AdminUserResponse toResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .kycStatus("NOT_SUBMITTED")
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
