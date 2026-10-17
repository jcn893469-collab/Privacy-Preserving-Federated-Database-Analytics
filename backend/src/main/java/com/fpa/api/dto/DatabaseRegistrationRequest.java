package com.fpa.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Organization database registration. Ownership is taken from the JWT, never
 * from a client-supplied ownerUserId.
 */
public class DatabaseRegistrationRequest {

    @NotBlank(message = "Organization name is required")
    @Size(max = 150, message = "Organization name must be 150 characters or fewer")
    private String organizationName;

    @NotBlank(message = "Database identifier is required")
    @Size(max = 100, message = "Database identifier must be 100 characters or fewer")
    private String dbIdentifier;

    private String description;

    public DatabaseRegistrationRequest() {
    }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    public String getDbIdentifier() { return dbIdentifier; }
    public void setDbIdentifier(String dbIdentifier) { this.dbIdentifier = dbIdentifier; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
