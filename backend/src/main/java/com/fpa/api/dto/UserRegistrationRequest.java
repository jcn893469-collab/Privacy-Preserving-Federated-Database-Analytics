package com.fpa.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Public self-registration request.
 *
 * SECURITY NOTE: the "role" field here is intentionally still free text,
 * but UserService#registerUser (public path) now hard-rejects any value
 * that resolves to ADMIN. Admin accounts can only be created by an existing
 * ADMIN via UserService#registerUserAsAdmin (see AdminController), never
 * through this public endpoint.
 */
public class UserRegistrationRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name must be 150 characters or fewer")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must be 255 characters or fewer")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Role is required")
    private String role;

    public UserRegistrationRequest() {
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}