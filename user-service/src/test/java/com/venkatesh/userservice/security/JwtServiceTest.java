package com.venkatesh.userservice.security;

import com.venkatesh.userservice.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    @Mock private JwtProperties jwtProperties;
    @InjectMocks private JwtService jwtService;

    private UserDetails userDetails;

    // 32-byte ASCII key (256-bit) required for HS256
    private static final String SECRET = "12345678901234567890123456789012";
    private static final long EXPIRY_MS = 86_400_000L; // 24 hours

    @BeforeEach
    void setUp() {
        given(jwtProperties.getSecret()).willReturn(SECRET);
        given(jwtProperties.getExpirationMs()).willReturn(EXPIRY_MS);

        userDetails = new User(
                "venkatesh@example.com",
                "$2a$10$hashed",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("✅ generates a non-null, non-blank token")
    void shouldGenerateToken() {
        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("✅ extracts correct email from token")
    void shouldExtractEmailFromToken() {
        String token = jwtService.generateToken(userDetails);

        String email = jwtService.extractEmail(token);

        assertThat(email).isEqualTo("venkatesh@example.com");
    }

    @Test
    @DisplayName("✅ validates a freshly generated token")
    void shouldValidateFreshToken() {
        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("❌ rejects token for a different user")
    void shouldRejectTokenForDifferentUser() {
        String token = jwtService.generateToken(userDetails);

        UserDetails otherUser = new User("other@example.com", "pass", List.of());

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    @DisplayName("❌ rejects an expired token")
    void shouldRejectExpiredToken() {
        given(jwtProperties.getExpirationMs()).willReturn(-1000L); // expired 1 second ago

        String expiredToken = jwtService.generateToken(userDetails);

        assertThat(jwtService.isTokenValid(expiredToken, userDetails)).isFalse();
    }

    @Test
    @DisplayName("❌ rejects a tampered token")
    void shouldRejectTamperedToken() {
        String token = jwtService.generateToken(userDetails);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(jwtService.isTokenValid(tampered, userDetails)).isFalse();
    }
}

