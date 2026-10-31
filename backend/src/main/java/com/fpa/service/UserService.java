package com.fpa.service;

import com.fpa.api.dto.AdminCreateUserRequest;
import com.fpa.api.dto.PublicRegistrationRequest;
import com.fpa.api.dto.UserRegistrationRequest;
import com.fpa.api.dto.UserResponse;
import com.fpa.domain.entity.User;
import com.fpa.domain.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse registerPublic(PublicRegistrationRequest request) {
        return createUser(request.getFullName(), request.getEmail(), request.getPassword(), User.Role.DATA_OWNER);
    }

    @Transactional
    public UserResponse registerUser(UserRegistrationRequest request) {
        User.Role role = parseRole(request.getRole());
        if (role == User.Role.ADMIN) {
            throw new IllegalArgumentException("Public registration cannot create ADMIN accounts");
        }
        return createUser(request.getFullName(), request.getEmail(), request.getPassword(), role);
    }

    @Transactional
    public UserResponse registerUserAsAdmin(AdminCreateUserRequest request) {
        User.Role role = parseRole(request.getRole());
        return createUser(request.getFullName(), request.getEmail(), request.getPassword(), role);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getPaginatedUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> searchAndFilter(String query, String roleStr, Boolean active, Pageable pageable) {
        User.Role role = (roleStr != null && !roleStr.isBlank()) ? parseRole(roleStr) : null;
        String normalizedQuery = query == null || query.isBlank() ? "" : query.trim();
        return userRepository.searchAndFilter(
            normalizedQuery, role != null, role == null ? User.Role.ADMIN : role,
            active != null, active != null && active, pageable
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return toResponse(userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id)));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        return toResponse(userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email)));
    }

    @Transactional
    public UserResponse updateUser(Long id, String fullName, String roleStr, Boolean active) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        User.Role originalRole = user.getRole();
        boolean originalActive = user.isActive();

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        if (roleStr != null && !roleStr.isBlank()) {
            User.Role newRole = parseRole(roleStr);
            if (user.getRole() == User.Role.ADMIN && newRole != User.Role.ADMIN) {
                long adminCount = userRepository.countByRole(User.Role.ADMIN);
                if (adminCount <= 1) {
                    throw new IllegalArgumentException("Cannot change role of the last ADMIN account");
                }
            }
            user.setRole(newRole);
        }
        if (active != null) {
            if (user.getRole() == User.Role.ADMIN && !active) {
                long adminCount = userRepository.countByRole(User.Role.ADMIN);
                if (adminCount <= 1) {
                    throw new IllegalArgumentException("Cannot deactivate the last ADMIN account");
                }
            }
            user.setActive(active);
        }
        if (user.getRole() != originalRole || user.isActive() != originalActive) {
            user.setTokenVersion(user.getTokenVersion() + 1);
        }
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void logout(Long userId) {
        if (userRepository.incrementTokenVersion(userId) != 1) {
            throw new IllegalStateException("Could not invalidate authentication tokens for user: " + userId);
        }
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        if (user.getRole() == User.Role.ADMIN) {
            long adminCount = userRepository.countByRole(User.Role.ADMIN);
            if (adminCount <= 1) {
                throw new IllegalArgumentException("Cannot delete the last ADMIN account");
            }
        }
        userRepository.delete(user);
    }

    @Transactional
    public UserResponse setActive(Long id, boolean active) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        if (user.getRole() == User.Role.ADMIN && !active) {
            long adminCount = userRepository.countByRole(User.Role.ADMIN);
            if (adminCount <= 1) {
                throw new IllegalArgumentException("Cannot deactivate the last ADMIN account");
            }
        }
        user.setActive(active);
        return toResponse(userRepository.save(user));
    }

    private UserResponse createUser(String fullName, String email, String password, User.Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("A user with this email already exists: " + email);
        }
        User user = User.builder()
            .fullName(fullName)
            .email(email.trim().toLowerCase())
            .passwordHash(passwordEncoder.encode(password))
            .role(role)
            .active(true)
            .createdAt(LocalDateTime.now())
            .build();
        return toResponse(userRepository.save(user));
    }

    private User.Role parseRole(String raw) {
        try {
            return User.Role.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("Invalid role: " + raw + ". Must be one of ADMIN, DATA_OWNER, RESEARCHER");
        }
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.getRole().name(),
            user.isActive(),
            user.getCreatedAt()
        );
    }
}
