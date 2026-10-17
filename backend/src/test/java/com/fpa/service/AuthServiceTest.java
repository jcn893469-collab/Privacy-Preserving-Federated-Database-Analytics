package com.fpa.service;

import com.fpa.api.dto.LoginRequest;
import com.fpa.api.dto.LoginResponse;
import com.fpa.domain.entity.User;
import com.fpa.domain.repository.UserRepository;
import com.fpa.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void testSuccessfulLogin() {
        User user = User.builder()
                .fullName("Alice Owner")
                .email("alice@hospital.org")
                .passwordHash("hashed_pw")
                .role(User.Role.DATA_OWNER)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        user.setId(10L);

        when(userRepository.findByEmail("alice@hospital.org")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("ValidPw@123", "hashed_pw")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("mocked_jwt_token");

        LoginRequest request = new LoginRequest();
        request.setEmail("alice@hospital.org");
        request.setPassword("ValidPw@123");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked_jwt_token", response.getToken());
        assertEquals("alice@hospital.org", response.getEmail());
        assertEquals("DATA_OWNER", response.getRole());
    }

    @Test
    void testLoginWithWrongPasswordThrowsBadCredentials() {
        User user = User.builder()
                .fullName("Alice Owner")
                .email("alice@hospital.org")
                .passwordHash("hashed_pw")
                .role(User.Role.DATA_OWNER)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("alice@hospital.org")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPw", "hashed_pw")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("alice@hospital.org");
        request.setPassword("WrongPw");

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void testLoginWithUnknownEmailThrowsBadCredentials() {
        when(userRepository.findByEmail("unknown@hospital.org")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@hospital.org");
        request.setPassword("AnyPassword");

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void testLoginWithInactiveAccountThrowsException() {
        User user = User.builder()
                .fullName("Inactive User")
                .email("inactive@hospital.org")
                .passwordHash("hashed_pw")
                .role(User.Role.DATA_OWNER)
                .active(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("inactive@hospital.org")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setEmail("inactive@hospital.org");
        request.setPassword("AnyPassword");

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}
