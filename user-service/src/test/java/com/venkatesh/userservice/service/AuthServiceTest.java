package com.venkatesh.userservice.service;

import com.venkatesh.userservice.dto.AuthResponse;
import com.venkatesh.userservice.dto.CreateUserRequest;
import com.venkatesh.userservice.dto.LoginRequest;
import com.venkatesh.userservice.exception.EmailAlreadyExistsException;
import com.venkatesh.userservice.model.User;
import com.venkatesh.userservice.repository.UserRepository;
import com.venkatesh.userservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @InjectMocks private AuthService authService;

    private User savedUser;
    private UserDetails userDetails;
    private static final String ENCODED_PASSWORD = "$2a$10$hashed";
    private static final String JWT_TOKEN = "eyJhbGciOiJIUzI1NiJ9.test.token";

    @BeforeEach
    void setUp() {
        savedUser = User.builder()
                .id(1L)
                .name("Venkatesh")
                .email("venkatesh@example.com")
                .password(ENCODED_PASSWORD)
                .build();

        userDetails = new org.springframework.security.core.userdetails.User(
                "venkatesh@example.com", ENCODED_PASSWORD, List.of());
    }

    // =========================================================================
    // register
    // =========================================================================
    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("✅ registers user and returns JWT token")
        void shouldRegisterAndReturnToken() {
            CreateUserRequest request = new CreateUserRequest();
            request.setName("Venkatesh");
            request.setEmail("venkatesh@example.com");
            request.setPassword("secret123");

            given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
            given(passwordEncoder.encode(request.getPassword())).willReturn(ENCODED_PASSWORD);
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(userDetailsService.loadUserByUsername(savedUser.getEmail())).willReturn(userDetails);
            given(jwtService.generateToken(userDetails)).willReturn(JWT_TOKEN);

            AuthResponse response = authService.register(request);

            assertThat(response.getAccessToken()).isEqualTo(JWT_TOKEN);
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getUser().getEmail()).isEqualTo("venkatesh@example.com");
        }

        @Test
        @DisplayName("❌ throws EmailAlreadyExistsException when email is taken")
        void shouldThrowWhenEmailAlreadyExists() {
            CreateUserRequest request = new CreateUserRequest();
            request.setEmail("venkatesh@example.com");
            request.setPassword("secret123");
            request.setName("Venkatesh");

            given(userRepository.existsByEmail(request.getEmail())).willReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(EmailAlreadyExistsException.class);

            then(userRepository).should(never()).save(any());
            then(jwtService).should(never()).generateToken(any(UserDetails.class));
        }
    }

    // =========================================================================
    // login
    // =========================================================================
    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("✅ authenticates and returns JWT token")
        void shouldLoginAndReturnToken() {
            LoginRequest request = new LoginRequest();
            request.setEmail("venkatesh@example.com");
            request.setPassword("secret123");

            given(userDetailsService.loadUserByUsername(request.getEmail())).willReturn(userDetails);
            given(jwtService.generateToken(userDetails)).willReturn(JWT_TOKEN);
            given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(savedUser));

            AuthResponse response = authService.login(request);

            assertThat(response.getAccessToken()).isEqualTo(JWT_TOKEN);
            assertThat(response.getUser().getId()).isEqualTo(1L);
            then(authenticationManager).should().authenticate(
                    any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("❌ throws BadCredentialsException on wrong password")
        void shouldThrowOnWrongCredentials() {
            LoginRequest request = new LoginRequest();
            request.setEmail("venkatesh@example.com");
            request.setPassword("wrongpassword");

            given(authenticationManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);

            then(jwtService).should(never()).generateToken(any(UserDetails.class));
        }
    }
}

