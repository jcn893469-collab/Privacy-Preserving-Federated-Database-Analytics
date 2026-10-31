package com.fpa.security;

import com.fpa.domain.entity.User;
import org.junit.jupiter.api.Test;

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
    void rejectsMissingOrShortSigningSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtService("", 60_000));
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 60_000));
    }

    @Test
    void rejectsNonPositiveTokenLifetime() {
        assertThrows(IllegalStateException.class, () -> new JwtService(TEST_SECRET, 0));
    }
}
