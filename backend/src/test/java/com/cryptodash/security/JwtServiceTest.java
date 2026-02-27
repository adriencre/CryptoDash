package com.cryptodash.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "test-secret-key-min-256-bits-for-hs256-must-be-long-enough";
    private final long expirationMs = 3600000;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, expirationMs);
    }

    @Test
    void shouldGenerateAndParseToken() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";

        String token = jwtService.generateToken(userId, email);

        assertThat(token).isNotNull();

        Claims claims = jwtService.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email")).isEqualTo(email);
    }

    @Test
    void shouldGetUserIdFromToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "test@example.com");

        UUID extractedUserId = jwtService.getUserIdFromToken(token);

        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void shouldHandleTempToken() {
        UUID userId = UUID.randomUUID();
        String tempToken = jwtService.generateTempToken(userId);
        String regularToken = jwtService.generateToken(userId, "test@example.com");

        assertThat(jwtService.isTempToken(tempToken)).isTrue();
        assertThat(jwtService.isTempToken(regularToken)).isFalse();
    }
}
