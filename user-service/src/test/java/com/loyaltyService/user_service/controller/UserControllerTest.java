package com.loyaltyService.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyService.user_service.dto.UpdateUserRequest;
import com.loyaltyService.user_service.dto.UserProfileResponse;
import com.loyaltyService.user_service.entity.User;
import com.loyaltyService.user_service.service.UserCommandService;
import com.loyaltyService.user_service.service.UserQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private UserCommandService userCommandService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    void testGetProfile() throws Exception {
        UserProfileResponse mockRes = new UserProfileResponse();
        mockRes.setId(1L);
        mockRes.setName("Test User");

        when(userQueryService.getProfile(1L)).thenReturn(mockRes);

        mockMvc.perform(get("/api/users/profile")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Test User"));
    }

    @Test
    void testUpdateProfile() throws Exception {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setName("Updated Name");
        req.setPhone("1234567890");

        UserProfileResponse mockRes = new UserProfileResponse();
        mockRes.setName("Updated Name");

        when(userCommandService.updateProfile(eq(1L), any(UpdateUserRequest.class))).thenReturn(mockRes);

        mockMvc.perform(put("/api/users/profile")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Name"));
    }

    @Test
    void testCreateFromAuth() throws Exception {
        String jsonPayload = """
                {
                    "id": 1,
                    "name": "New User",
                    "email": "new@test.com",
                    "phone": "9998887776",
                    "role": "USER"
                }
                """;

        mockMvc.perform(post("/api/users/internal/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isOk());

        verify(userCommandService, times(1)).createUser(1L, "New User", "new@test.com", "9998887776", User.Role.USER);
    }
}
