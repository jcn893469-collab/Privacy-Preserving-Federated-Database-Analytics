package com.fpa.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "federated_database")
public class FederatedDatabase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_name", nullable = false, length = 150)
    private String organizationName;

    @Column(name = "db_identifier", nullable = false, unique = true, length = 100)
    private String dbIdentifier;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @Column(name = "privacy_epsilon_budget", nullable = false, precision = 10, scale = 4)
    private BigDecimal privacyEpsilonBudget = BigDecimal.valueOf(10.0);

    @Column(name = "privacy_delta_budget", nullable = false, precision = 20, scale = 15)
    private BigDecimal privacyDeltaBudget = BigDecimal.valueOf(0.00001);

    @Column(name = "privacy_epsilon_spent", nullable = false, precision = 10, scale = 4)
    private BigDecimal privacyEpsilonSpent = BigDecimal.ZERO;

    @Column(name = "rdp_accountant_state", columnDefinition = "TEXT")
    private String rdpAccountantState;

    public FederatedDatabase() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    public String getDbIdentifier() { return dbIdentifier; }
    public void setDbIdentifier(String dbIdentifier) { this.dbIdentifier = dbIdentifier; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    public BigDecimal getPrivacyEpsilonBudget() { return privacyEpsilonBudget; }
    public void setPrivacyEpsilonBudget(BigDecimal privacyEpsilonBudget) { this.privacyEpsilonBudget = privacyEpsilonBudget; }
    public BigDecimal getPrivacyDeltaBudget() { return privacyDeltaBudget; }
    public void setPrivacyDeltaBudget(BigDecimal privacyDeltaBudget) { this.privacyDeltaBudget = privacyDeltaBudget; }
    public BigDecimal getPrivacyEpsilonSpent() { return privacyEpsilonSpent; }
    public void setPrivacyEpsilonSpent(BigDecimal privacyEpsilonSpent) { this.privacyEpsilonSpent = privacyEpsilonSpent; }
    public String getRdpAccountantState() { return rdpAccountantState; }
    public void setRdpAccountantState(String rdpAccountantState) { this.rdpAccountantState = rdpAccountantState; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final FederatedDatabase target = new FederatedDatabase();

        public Builder organizationName(String organizationName) { target.organizationName = organizationName; return this; }
        public Builder dbIdentifier(String dbIdentifier) { target.dbIdentifier = dbIdentifier; return this; }
        public Builder description(String description) { target.description = description; return this; }
        public Builder owner(User owner) { target.owner = owner; return this; }
        public Builder active(boolean active) { target.active = active; return this; }
        public Builder registeredAt(LocalDateTime registeredAt) { target.registeredAt = registeredAt; return this; }
        public Builder privacyEpsilonBudget(BigDecimal privacyEpsilonBudget) { target.privacyEpsilonBudget = privacyEpsilonBudget; return this; }
        public Builder privacyDeltaBudget(BigDecimal privacyDeltaBudget) { target.privacyDeltaBudget = privacyDeltaBudget; return this; }
        public Builder privacyEpsilonSpent(BigDecimal privacyEpsilonSpent) { target.privacyEpsilonSpent = privacyEpsilonSpent; return this; }
        public Builder rdpAccountantState(String rdpAccountantState) { target.rdpAccountantState = rdpAccountantState; return this; }
        public FederatedDatabase build() { return target; }
    }
}
