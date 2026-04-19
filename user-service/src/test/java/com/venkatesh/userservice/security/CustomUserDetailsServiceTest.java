package com.venkatesh.userservice.security;

import com.venkatesh.userservice.model.User;
import com.venkatesh.userservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Unit Tests")
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private CustomUserDetailsService service;

    @Test
    @DisplayName("✅ loads UserDetails by email successfully")
    void shouldLoadUserByEmail() {
        User user = User.builder()
                .id(1L).name("Venkatesh")
                .email("venkatesh@example.com")
                .password("$2a$10$hashed")
                .build();

        given(userRepository.findByEmail("venkatesh@example.com"))
                .willReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("venkatesh@example.com");

        assertThat(details.getUsername()).isEqualTo("venkatesh@example.com");
        assertThat(details.getPassword()).isEqualTo("$2a$10$hashed");
        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("❌ throws UsernameNotFoundException when email not found")
    void shouldThrowWhenEmailNotFound() {
        given(userRepository.findByEmail("missing@example.com"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("missing@example.com");
    }
}

