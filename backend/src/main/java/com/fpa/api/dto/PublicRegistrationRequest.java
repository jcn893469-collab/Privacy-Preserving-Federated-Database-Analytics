package com.fpa.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PublicRegistrationRequest {

    @NotBlank
    @Size(max = 150)
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(min = 8)
    private String password;

    @NotBlank
    @Size(max = 150)
    private String organizationName;

    @Size(max = 100)
    private String dbIdentifier;

    private String description;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    public String getDbIdentifier() { return dbIdentifier; }
    public void setDbIdentifier(String dbIdentifier) { this.dbIdentifier = dbIdentifier; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
