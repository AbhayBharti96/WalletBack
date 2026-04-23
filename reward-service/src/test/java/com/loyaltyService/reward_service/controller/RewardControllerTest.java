package com.loyaltyService.reward_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyService.reward_service.dto.RedeemRequest;
import com.loyaltyService.reward_service.dto.RewardItemRequest;
import com.loyaltyService.reward_service.dto.RewardSummaryDto;
import com.loyaltyService.reward_service.entity.RewardItem;
import com.loyaltyService.reward_service.service.RewardCommandService;
import com.loyaltyService.reward_service.service.RewardQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RewardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RewardQueryService rewardQueryService;

    @Mock
    private RewardCommandService rewardCommandService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RewardController rewardController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(rewardController).build();
    }

    @Test
    void testGetSummary() throws Exception {
        RewardSummaryDto summary = RewardSummaryDto.builder()
                .userId(1L)
                .points(500)
                .tier("SILVER")
                .nextTier("GOLD")
                .pointsToNextTier(500)
                .build();

        when(rewardQueryService.getSummary(1L)).thenReturn(summary);

        mockMvc.perform(get("/api/rewards/summary")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.points").value(500))
                .andExpect(jsonPath("$.data.tier").value("SILVER"));
    }

    @Test
    void testRedeemPoints() throws Exception {
        mockMvc.perform(post("/api/rewards/redeem-points")
                .header("X-User-Id", "1")
                .queryParam("points", "200")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(rewardCommandService, times(1)).redeemPoints(1L, 200);
    }

    @Test
    void testAdminCatalogRead_ForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/rewards/admin/catalog")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied — ADMIN role required"));
    }

    @Test
    void testAdminCatalogRead_SuccessForAdmin() throws Exception {
        when(rewardQueryService.getCatalogForAdmin()).thenReturn(List.of(
                RewardItem.builder().id(1L).name("Coupon").pointsRequired(500).active(true).build()
        ));

        mockMvc.perform(get("/api/rewards/admin/catalog")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Admin catalog fetched"))
                .andExpect(jsonPath("$.data[0].name").value("Coupon"));
    }

    @Test
    void testUpdateCatalogItem_SuccessForAdmin() throws Exception {
        RewardItemRequest request = new RewardItemRequest(
                "Voucher",
                "Updated desc",
                700,
                "VOUCHER",
                10,
                "SILVER",
                BigDecimal.ZERO
        );
        when(rewardCommandService.updateCatalogItem(eq(10L), any(RewardItemRequest.class)))
                .thenReturn(RewardItem.builder().id(10L).name("Voucher").pointsRequired(700).active(true).build());

        mockMvc.perform(put("/api/rewards/admin/catalog/10")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reward item updated"))
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    void testDeleteCatalogItem_ForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(delete("/api/rewards/admin/catalog/10")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());

        verify(rewardCommandService, never()).deleteCatalogItem(anyLong());
    }
}
