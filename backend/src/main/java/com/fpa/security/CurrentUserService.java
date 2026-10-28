package com.fpa.security;

import com.fpa.domain.entity.User;
import com.fpa.domain.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

@Component
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown account"));
    }

    public User requireRole(User.Role role) {
        return requireAnyRole(role);
    }

    public User requireAnyRole(User.Role... roles) {
        User user = requireUser();
        if (Arrays.stream(roles).noneMatch(role -> role == user.getRole())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "This action requires one of the roles " + Arrays.toString(roles)
            );
        }
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is inactive");
        }
        return user;
    }
}
