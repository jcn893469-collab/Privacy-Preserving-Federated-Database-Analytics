package com.fpa.api.controller;

import com.fpa.api.dto.LoginRequest;
import com.fpa.api.dto.LoginResponse;
import com.fpa.api.dto.UserResponse;
import com.fpa.domain.entity.User;
import com.fpa.security.CurrentUserService;
import com.fpa.service.AuthService;
import com.fpa.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final CurrentUserService currentUserService;

    public AuthController(AuthService authService, UserService userService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.userService = userService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        User user = currentUserService.requireUser();
        UserResponse response = userService.toResponse(user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        User user = currentUserService.requireUser();
        userService.logout(user.getId());
        return ResponseEntity.ok(Map.of("message", "All active tokens for this account have been invalidated"));
    }
}