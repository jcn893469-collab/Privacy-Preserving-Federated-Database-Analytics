package com.fpa.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DatabaseResponse {

    private Long id;
    private String organizationName;
    private String dbIdentifier;
    private String description;
    private Long ownerUserId;
    private String ownerEmail;
    private boolean active;
    private LocalDateTime registeredAt;
    private BigDecimal privacyEpsilonBudget;
    private BigDecimal privacyEpsilonSpent;
    private BigDecimal privacyDeltaBudget;

    public DatabaseResponse() {
    }

    public DatabaseResponse(Long id, String organizationName, String dbIdentifier, String description,
                             Long ownerUserId, String ownerEmail, boolean active, LocalDateTime registeredAt,
                             BigDecimal privacyEpsilonBudget, BigDecimal privacyEpsilonSpent,
                             BigDecimal privacyDeltaBudget) {
        this.id = id;
        this.organizationName = organizationName;
        this.dbIdentifier = dbIdentifier;
        this.description = description;
        this.ownerUserId = ownerUserId;
        this.ownerEmail = ownerEmail;
        this.active = active;
        this.registeredAt = registeredAt;
        this.privacyEpsilonBudget = privacyEpsilonBudget;
        this.privacyEpsilonSpent = privacyEpsilonSpent;
        this.privacyDeltaBudget = privacyDeltaBudget;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getDbIdentifier() {
        return dbIdentifier;
    }

    public void setDbIdentifier(String dbIdentifier) {
        this.dbIdentifier = dbIdentifier;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public BigDecimal getPrivacyEpsilonBudget() {
        return privacyEpsilonBudget;
    }

    public void setPrivacyEpsilonBudget(BigDecimal privacyEpsilonBudget) {
        this.privacyEpsilonBudget = privacyEpsilonBudget;
    }

    public BigDecimal getPrivacyEpsilonSpent() {
        return privacyEpsilonSpent;
    }

    public void setPrivacyEpsilonSpent(BigDecimal privacyEpsilonSpent) {
        this.privacyEpsilonSpent = privacyEpsilonSpent;
    }

    public BigDecimal getPrivacyDeltaBudget() {
        return privacyDeltaBudget;
    }

    public void setPrivacyDeltaBudget(BigDecimal privacyDeltaBudget) {
        this.privacyDeltaBudget = privacyDeltaBudget;
    }
}