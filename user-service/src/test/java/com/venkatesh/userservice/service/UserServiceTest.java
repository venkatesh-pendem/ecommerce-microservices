package com.venkatesh.userservice.service;

import com.venkatesh.userservice.dto.CreateUserRequest;
import com.venkatesh.userservice.dto.PagedResponse;
import com.venkatesh.userservice.dto.UserResponse;
import com.venkatesh.userservice.exception.EmailAlreadyExistsException;
import com.venkatesh.userservice.exception.UserNotFoundException;
import com.venkatesh.userservice.model.User;
import com.venkatesh.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;

    private User user;
    private CreateUserRequest request;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Venkatesh")
                .email("venkatesh@example.com")
                .password("$2a$10$hashedpassword")
                .build();

        request = new CreateUserRequest();
        request.setName("Venkatesh");
        request.setEmail("venkatesh@example.com");
        request.setPassword("secret123");
    }

    // =========================================================================
    // createUser
    // =========================================================================
    @Nested
    @DisplayName("createUser()")
    class CreateUser {

        @Test
        @DisplayName("✅ creates user and returns response when email is unique")
        void shouldCreateUserSuccessfully() {
            given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
            given(passwordEncoder.encode(request.getPassword())).willReturn("$2a$10$hashed");
            given(userRepository.save(any(User.class))).willReturn(user);

            UserResponse response = userService.createUser(request);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("Venkatesh");
            assertThat(response.getEmail()).isEqualTo("venkatesh@example.com");
            then(userRepository).should().save(any(User.class));
        }

        @Test
        @DisplayName("❌ throws EmailAlreadyExistsException when email is duplicate")
        void shouldThrowWhenEmailAlreadyExists() {
            given(userRepository.existsByEmail(request.getEmail())).willReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining("venkatesh@example.com");

            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("✅ password is BCrypt-encoded before saving")
        void shouldEncodePasswordBeforeSaving() {
            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(passwordEncoder.encode("secret123")).willReturn("$2a$10$hashed");
            given(userRepository.save(any(User.class))).willReturn(user);

            userService.createUser(request);

            then(passwordEncoder).should().encode("secret123");
        }
    }

    // =========================================================================
    // getAllUsers (paginated)
    // =========================================================================
    @Nested
    @DisplayName("getAllUsers()")
    class GetAllUsers {

        @Test
        @DisplayName("✅ returns paginated result with correct metadata")
        void shouldReturnPagedResponse() {
            Page<User> page = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
            given(userRepository.findAll(any(Pageable.class))).willReturn(page);

            PagedResponse<UserResponse> result = userService.getAllUsers(0, 10, "id", "asc");

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getPage()).isZero();
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.isLast()).isTrue();
        }

        @Test
        @DisplayName("✅ caps page size at MAX (100)")
        void shouldCapPageSizeAt100() {
            Page<User> page = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
            given(userRepository.findAll(any(Pageable.class))).willReturn(page);

            userService.getAllUsers(0, 999, "id", "asc");

            then(userRepository).should().findAll(
                    argThat((Pageable p) -> p.getPageSize() == 100));
        }

        @Test
        @DisplayName("✅ applies DESC sort correctly")
        void shouldApplyDescSort() {
            Page<User> page = new PageImpl<>(List.of(), PageRequest.of(0, 10, Sort.by("name").descending()), 0);
            given(userRepository.findAll(any(Pageable.class))).willReturn(page);

            userService.getAllUsers(0, 10, "name", "desc");

            then(userRepository).should().findAll(
                    argThat((Pageable p) -> p.getSort().getOrderFor("name") != null
                            && p.getSort().getOrderFor("name").isDescending()));
        }
    }

    // =========================================================================
    // getUserById
    // =========================================================================
    @Nested
    @DisplayName("getUserById()")
    class GetUserById {

        @Test
        @DisplayName("✅ returns user when id exists")
        void shouldReturnUserById() {
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            UserResponse response = userService.getUserById(1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getEmail()).isEqualTo("venkatesh@example.com");
        }

        @Test
        @DisplayName("❌ throws UserNotFoundException when id does not exist")
        void shouldThrowWhenUserNotFound() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(99L))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // =========================================================================
    // deleteUser
    // =========================================================================
    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        @DisplayName("✅ deletes user when id exists")
        void shouldDeleteUserSuccessfully() {
            given(userRepository.existsById(1L)).willReturn(true);

            userService.deleteUser(1L);

            then(userRepository).should().deleteById(1L);
        }

        @Test
        @DisplayName("❌ throws UserNotFoundException when id does not exist")
        void shouldThrowWhenUserNotFound() {
            given(userRepository.existsById(99L)).willReturn(false);

            assertThatThrownBy(() -> userService.deleteUser(99L))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("99");

            then(userRepository).should(never()).deleteById(any());
        }
    }
}

