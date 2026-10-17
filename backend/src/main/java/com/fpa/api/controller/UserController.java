package com.fpa.api.controller;

import com.fpa.api.dto.AdminCreateUserRequest;
import com.fpa.api.dto.PublicRegistrationRequest;
import com.fpa.api.dto.UserRegistrationRequest;
import com.fpa.api.dto.UserResponse;
import com.fpa.domain.entity.User;
import com.fpa.security.CurrentUserService;
import com.fpa.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    public UserController(UserService userService, CurrentUserService currentUserService) {
        this.userService = userService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerPublic(@Valid @RequestBody PublicRegistrationRequest request) {
        UserResponse response = userService.registerPublic(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/admin")
    public ResponseEntity<?> createUserByAdmin(@Valid @RequestBody AdminCreateUserRequest request) {
        currentUserService.requireRole(User.Role.ADMIN);
        UserResponse response = userService.registerUserAsAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (query != null || role != null || active != null) {
            return ResponseEntity.ok(userService.searchAndFilter(query, role, active, pageable));
        }
        return ResponseEntity.ok(userService.getPaginatedUsers(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        currentUserService.requireRole(User.Role.ADMIN);
        String fullName = (String) body.get("fullName");
        String roleStr = (String) body.get("role");
        Boolean active = (Boolean) body.get("active");
        UserResponse response = userService.updateUser(id, fullName, roleStr, active);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        currentUserService.requireRole(User.Role.ADMIN);
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully", "id", id));
    }
}