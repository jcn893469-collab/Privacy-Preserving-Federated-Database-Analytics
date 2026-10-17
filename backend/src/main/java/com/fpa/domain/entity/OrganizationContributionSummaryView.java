package com.fpa.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "v_organization_contribution_summary")
public class OrganizationContributionSummaryView {

    @Id
    @Column(name = "database_id")
    private Long databaseId;

    @Column(name = "organization_name")
    private String organizationName;

    @Column(name = "db_identifier")
    private String dbIdentifier;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "owner_email")
    private String ownerEmail;

    @Column(name = "owner_name")
    private String ownerName;

    @Column(name = "contribution_count")
    private Long contributionCount;

    @Column(name = "total_records_protected")
    private Long totalRecordsProtected;

    @Column(name = "total_noisy_sum")
    private BigDecimal totalNoisySum;

    @Column(name = "avg_records_per_contribution")
    private BigDecimal avgRecordsPerContribution;

    @Column(name = "avg_anomaly_rate")
    private BigDecimal avgAnomalyRate;

    @Column(name = "total_anomalies_flagged")
    private Long totalAnomaliesFlagged;

    @Column(name = "privacy_epsilon_budget")
    private BigDecimal privacyEpsilonBudget;

    @Column(name = "privacy_epsilon_spent")
    private BigDecimal privacyEpsilonSpent;

    @Column(name = "privacy_budget_remaining")
    private BigDecimal privacyBudgetRemaining;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @Column(name = "last_contribution_at")
    private LocalDateTime lastContributionAt;

    public OrganizationContributionSummaryView() {
    }

    public Long getDatabaseId() { return databaseId; }
    public String getOrganizationName() { return organizationName; }
    public String getDbIdentifier() { return dbIdentifier; }
    public boolean isActive() { return active; }
    public String getOwnerEmail() { return ownerEmail; }
    public String getOwnerName() { return ownerName; }
    public Long getContributionCount() { return contributionCount; }
    public Long getTotalRecordsProtected() { return totalRecordsProtected; }
    public BigDecimal getTotalNoisySum() { return totalNoisySum; }
    public BigDecimal getAvgRecordsPerContribution() { return avgRecordsPerContribution; }
    public BigDecimal getAvgAnomalyRate() { return avgAnomalyRate; }
    public Long getTotalAnomaliesFlagged() { return totalAnomaliesFlagged; }
    public BigDecimal getPrivacyEpsilonBudget() { return privacyEpsilonBudget; }
    public BigDecimal getPrivacyEpsilonSpent() { return privacyEpsilonSpent; }
    public BigDecimal getPrivacyBudgetRemaining() { return privacyBudgetRemaining; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public LocalDateTime getLastContributionAt() { return lastContributionAt; }
}
