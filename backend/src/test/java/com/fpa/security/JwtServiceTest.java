package com.fpa.security;

import com.fpa.domain.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String TEST_SECRET = "unit-test-secret-key-at-least-32-bytes";

    @Test
    void tokenIsValidForTheExpectedEmail() {
        JwtService jwtService = new JwtService(TEST_SECRET, 60_000);
        User user = User.builder().email("owner@example.org").role(User.Role.DATA_OWNER).build();
        user.setId(42L);

        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, "owner@example.org", 0));
        assertFalse(jwtService.isTokenValid(token, "other@example.org", 0));
        assertFalse(jwtService.isTokenValid(token, "owner@example.org", 1));
    }

    @Test
    void rejectsMalformedTokensAndTokensSignedByAnotherKey() {
        JwtService issuer = new JwtService(TEST_SECRET, 60_000);
        JwtService validator = new JwtService("different-test-secret-key-at-least-32-bytes", 60_000);
        User user = User.builder().email("owner@example.org").role(User.Role.DATA_OWNER).build();
        user.setId(42L);
        String token = issuer.generateToken(user);

        assertFalse(issuer.isTokenValid("not-a-jwt", user.getEmail(), 0));
        assertFalse(validator.isTokenValid(token, user.getEmail(), 0));
    }

    @Test
    void rejectsExpiredTokens() {
        JwtService jwtService = new JwtService(TEST_SECRET, 60_000);
        Date now = new Date();
        String expiredToken = Jwts.builder()
            .subject("owner@example.org")
            .claim("tokenVersion", 0)
            .issuedAt(new Date(now.getTime() - 2_000))
            .expiration(new Date(now.getTime() - 1_000))
            .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();

        assertFalse(jwtService.isTokenValid(expiredToken, "owner@example.org", 0));
    }

    @Test
    void rejectsMissingOrShortSigningSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtService("", 60_000));
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 60_000));
    }

    @Test
    void rejectsNonPositiveTokenLifetime() {
        assertThrows(IllegalStateException.class, () -> new JwtService(TEST_SECRET, 0));
    }
}
