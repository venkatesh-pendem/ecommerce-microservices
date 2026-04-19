package com.venkatesh.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.venkatesh.userservice.dto.*;
import com.venkatesh.userservice.exception.EmailAlreadyExistsException;
import com.venkatesh.userservice.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "app.jwt.secret=12345678901234567890123456789012",
        "app.jwt.expiration-ms=86400000"
})
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private AuthService authService;

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL    = "/api/v1/auth/login";

    private AuthResponse fakeAuthResponse() {
        UserResponse user = UserResponse.builder()
                .id(1L).name("Venkatesh").email("venkatesh@example.com").build();
        return AuthResponse.builder()
                .accessToken("eyJhbGci.test.token")
                .tokenType("Bearer")
                .user(user)
                .build();
    }

    // =========================================================================
    // POST /api/v1/auth/register
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("✅ 201 with token on successful registration")
        void shouldReturn201WithToken() throws Exception {
            CreateUserRequest req = new CreateUserRequest();
            req.setName("Venkatesh");
            req.setEmail("venkatesh@example.com");
            req.setPassword("secret123");

            given(authService.register(any())).willReturn(fakeAuthResponse());

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.user.email").value("venkatesh@example.com"));
        }

        @Test
        @DisplayName("❌ 409 when email is already taken")
        void shouldReturn409WhenEmailTaken() throws Exception {
            CreateUserRequest req = new CreateUserRequest();
            req.setName("Venkatesh");
            req.setEmail("venkatesh@example.com");
            req.setPassword("secret123");

            given(authService.register(any()))
                    .willThrow(new EmailAlreadyExistsException("venkatesh@example.com"));

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("❌ 400 when all fields are blank")
        void shouldReturn400WhenAllFieldsBlank() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.name").exists())
                    .andExpect(jsonPath("$.data.email").exists())
                    .andExpect(jsonPath("$.data.password").exists());
        }
    }

    // =========================================================================
    // POST /api/v1/auth/login
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("✅ 200 with token on successful login")
        void shouldReturn200WithToken() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setEmail("venkatesh@example.com");
            req.setPassword("secret123");

            given(authService.login(any())).willReturn(fakeAuthResponse());

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
        }

        @Test
        @DisplayName("❌ 401 on wrong credentials")
        void shouldReturn401OnBadCredentials() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setEmail("venkatesh@example.com");
            req.setPassword("wrongpass");

            given(authService.login(any()))
                    .willThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        @DisplayName("❌ 400 when email and password are missing")
        void shouldReturn400WhenFieldsMissing() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.email").exists())
                    .andExpect(jsonPath("$.data.password").exists());
        }
    }
}

