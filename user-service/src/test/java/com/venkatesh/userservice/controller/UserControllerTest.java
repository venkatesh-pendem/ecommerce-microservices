package com.venkatesh.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.venkatesh.userservice.dto.*;
import com.venkatesh.userservice.exception.UserNotFoundException;
import com.venkatesh.userservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private UserService userService;

    private static final String BASE_URL = "/api/v1/users";

    // =========================================================================
    // POST /api/v1/users
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/users")
    class CreateUser {

        @Test
        @WithMockUser
        @DisplayName("✅ 201 when valid request")
        void shouldReturn201OnValidRequest() throws Exception {
            CreateUserRequest req = new CreateUserRequest();
            req.setName("Venkatesh");
            req.setEmail("venkatesh@example.com");
            req.setPassword("secret123");

            UserResponse userResponse = UserResponse.builder()
                    .id(1L).name("Venkatesh").email("venkatesh@example.com").build();

            given(userService.createUser(any())).willReturn(userResponse);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.email").value("venkatesh@example.com"))
                    .andExpect(jsonPath("$.data.password").doesNotExist()); // password never leaks
        }

        @Test
        @WithMockUser
        @DisplayName("❌ 400 when name is blank")
        void shouldReturn400WhenNameBlank() throws Exception {
            CreateUserRequest req = new CreateUserRequest();
            req.setName("");
            req.setEmail("v@example.com");
            req.setPassword("secret123");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data.name").exists());
        }

        @Test
        @WithMockUser
        @DisplayName("❌ 400 when email is invalid")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            CreateUserRequest req = new CreateUserRequest();
            req.setName("Venkatesh");
            req.setEmail("not-an-email");
            req.setPassword("secret123");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.email").exists());
        }

        @Test
        @WithMockUser
        @DisplayName("❌ 400 when password is too short")
        void shouldReturn400WhenPasswordTooShort() throws Exception {
            CreateUserRequest req = new CreateUserRequest();
            req.setName("Venkatesh");
            req.setEmail("v@example.com");
            req.setPassword("abc");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.password").exists());
        }

        @Test
        @DisplayName("❌ 403 when no auth token")
        void shouldReturn403WhenUnauthenticated() throws Exception {
            CreateUserRequest req = new CreateUserRequest();
            req.setName("Venkatesh");
            req.setEmail("v@e.com");
            req.setPassword("secret123");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // GET /api/v1/users
    // =========================================================================
    @Nested
    @DisplayName("GET /api/v1/users")
    class GetAllUsers {

        @Test
        @WithMockUser
        @DisplayName("✅ 200 with paged response")
        void shouldReturn200WithPagedResult() throws Exception {
            UserResponse userResponse = UserResponse.builder()
                    .id(1L).name("Venkatesh").email("venkatesh@example.com").build();

            PagedResponse<UserResponse> paged = PagedResponse.<UserResponse>builder()
                    .content(List.of(userResponse))
                    .page(0).size(10).totalElements(1).totalPages(1).last(true)
                    .build();

            given(userService.getAllUsers(0, 10, "id", "asc")).willReturn(paged);

            mockMvc.perform(get(BASE_URL)
                            .param("page", "0").param("size", "10")
                            .param("sortBy", "id").param("sortDir", "asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].id").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.last").value(true));
        }
    }

    // =========================================================================
    // GET /api/v1/users/{id}
    // =========================================================================
    @Nested
    @DisplayName("GET /api/v1/users/{id}")
    class GetUserById {

        @Test
        @WithMockUser
        @DisplayName("✅ 200 when user exists")
        void shouldReturn200WhenUserExists() throws Exception {
            UserResponse response = UserResponse.builder()
                    .id(1L).name("Venkatesh").email("v@example.com").build();

            given(userService.getUserById(1L)).willReturn(response);

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1));
        }

        @Test
        @WithMockUser
        @DisplayName("❌ 404 when user does not exist")
        void shouldReturn404WhenUserNotFound() throws Exception {
            given(userService.getUserById(99L)).willThrow(new UserNotFoundException(99L));

            mockMvc.perform(get(BASE_URL + "/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("99")));
        }
    }

    // =========================================================================
    // DELETE /api/v1/users/{id}
    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/v1/users/{id}")
    class DeleteUser {

        @Test
        @WithMockUser
        @DisplayName("✅ 204 when user deleted")
        void shouldReturn204OnDelete() throws Exception {
            willDoNothing().given(userService).deleteUser(1L);

            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser
        @DisplayName("❌ 404 when user does not exist")
        void shouldReturn404WhenDeletingMissingUser() throws Exception {
            willThrow(new UserNotFoundException(99L)).given(userService).deleteUser(99L);

            mockMvc.perform(delete(BASE_URL + "/99"))
                    .andExpect(status().isNotFound());
        }
    }
}

