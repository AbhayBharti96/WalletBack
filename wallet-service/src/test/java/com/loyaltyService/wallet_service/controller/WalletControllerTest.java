package com.loyaltyService.wallet_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyService.wallet_service.dto.TransferRequest;
import com.loyaltyService.wallet_service.dto.WalletBalanceResponse;
import com.loyaltyService.wallet_service.dto.WithdrawRequest;
import com.loyaltyService.wallet_service.service.WalletCommandService;
import com.loyaltyService.wallet_service.service.WalletQueryService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WalletQueryService walletQueryService;

    @Mock
    private WalletCommandService walletCommandService;

    @Mock
    private com.loyaltyService.wallet_service.repository.LedgerEntryRepository ledgerRepo;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private WalletController walletController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(walletController).build();
    }

    @Test
    void testGetBalance() throws Exception {
        WalletBalanceResponse res = WalletBalanceResponse.builder()
                .userId(100L)
                .balance(new BigDecimal("150.00"))
                .status("ACTIVE")
                .build();

        when(walletQueryService.getBalance(100L)).thenReturn(res);

        mockMvc.perform(get("/api/wallet/balance")
                .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(150.00));
    }

    @Test
    void testTransfer() throws Exception {
        TransferRequest req = new TransferRequest();
        req.setReceiverId(200L);
        req.setAmount(new BigDecimal("50.00"));
        req.setIdempotencyKey("TXN123");
        req.setDescription("Gift");

        mockMvc.perform(post("/api/wallet/transfer")
                .header("X-User-Id", "100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(walletCommandService, times(1)).transfer(100L, 200L, new BigDecimal("50.00"), "TXN123", "Gift");
    }

    @Test
    void testWithdraw() throws Exception {
        WithdrawRequest req = new WithdrawRequest();
        req.setAmount(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/wallet/withdraw")
                .header("X-User-Id", "100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(walletCommandService, times(1)).withdraw(100L, new BigDecimal("100.00"));
    }
}
