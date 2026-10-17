package com.fpa.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "federated_result")
public class FederatedResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_identifier", nullable = false, unique = true, length = 100)
    private String runIdentifier;

    @Column(name = "computation_type", nullable = false, length = 50)
    private String computationType = "FEDERATED_MEAN";

    @Column(name = "target_metric", nullable = false, length = 100)
    private String targetMetric = "overall";

    @Column(name = "participating_org_count", nullable = false)
    private Integer participatingOrgCount;

    @Column(name = "total_contribution_count", nullable = false)
    private Integer totalContributionCount;

    @Column(name = "protected_total_records", nullable = false)
    private Integer protectedTotalRecords;

    @Column(name = "protected_aggregate_sum", nullable = false, precision = 18, scale = 4)
    private BigDecimal protectedAggregateSum;

    @Column(name = "global_protected_mean", nullable = false, precision = 18, scale = 6)
    private BigDecimal globalProtectedMean;

    @Column(name = "privacy_mechanism", nullable = false, length = 50)
    private String privacyMechanism = "GAUSSIAN + RDP";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executed_by_user_id")
    private User executedBy;

    @Column(name = "executed_at", nullable = false, updatable = false)
    private LocalDateTime executedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FederatedRunParticipant> participants = new ArrayList<>();

    public FederatedResult() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRunIdentifier() { return runIdentifier; }
    public void setRunIdentifier(String runIdentifier) { this.runIdentifier = runIdentifier; }
    public String getComputationType() { return computationType; }
    public void setComputationType(String computationType) { this.computationType = computationType; }
    public String getTargetMetric() { return targetMetric; }
    public void setTargetMetric(String targetMetric) { this.targetMetric = targetMetric; }
    public Integer getParticipatingOrgCount() { return participatingOrgCount; }
    public void setParticipatingOrgCount(Integer participatingOrgCount) { this.participatingOrgCount = participatingOrgCount; }
    public Integer getTotalContributionCount() { return totalContributionCount; }
    public void setTotalContributionCount(Integer totalContributionCount) { this.totalContributionCount = totalContributionCount; }
    public Integer getProtectedTotalRecords() { return protectedTotalRecords; }
    public void setProtectedTotalRecords(Integer protectedTotalRecords) { this.protectedTotalRecords = protectedTotalRecords; }
    public BigDecimal getProtectedAggregateSum() { return protectedAggregateSum; }
    public void setProtectedAggregateSum(BigDecimal protectedAggregateSum) { this.protectedAggregateSum = protectedAggregateSum; }
    public BigDecimal getGlobalProtectedMean() { return globalProtectedMean; }
    public void setGlobalProtectedMean(BigDecimal globalProtectedMean) { this.globalProtectedMean = globalProtectedMean; }
    public String getPrivacyMechanism() { return privacyMechanism; }
    public void setPrivacyMechanism(String privacyMechanism) { this.privacyMechanism = privacyMechanism; }
    public User getExecutedBy() { return executedBy; }
    public void setExecutedBy(User executedBy) { this.executedBy = executedBy; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
    public List<FederatedRunParticipant> getParticipants() { return participants; }
    public void setParticipants(List<FederatedRunParticipant> participants) { this.participants = participants; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final FederatedResult target = new FederatedResult();

        public Builder runIdentifier(String runIdentifier) { target.runIdentifier = runIdentifier; return this; }
        public Builder computationType(String computationType) { target.computationType = computationType; return this; }
        public Builder targetMetric(String targetMetric) { target.targetMetric = targetMetric; return this; }
        public Builder participatingOrgCount(Integer participatingOrgCount) { target.participatingOrgCount = participatingOrgCount; return this; }
        public Builder totalContributionCount(Integer totalContributionCount) { target.totalContributionCount = totalContributionCount; return this; }
        public Builder protectedTotalRecords(Integer protectedTotalRecords) { target.protectedTotalRecords = protectedTotalRecords; return this; }
        public Builder protectedAggregateSum(BigDecimal protectedAggregateSum) { target.protectedAggregateSum = protectedAggregateSum; return this; }
        public Builder globalProtectedMean(BigDecimal globalProtectedMean) { target.globalProtectedMean = globalProtectedMean; return this; }
        public Builder privacyMechanism(String privacyMechanism) { target.privacyMechanism = privacyMechanism; return this; }
        public Builder executedBy(User executedBy) { target.executedBy = executedBy; return this; }
        public Builder executedAt(LocalDateTime executedAt) { target.executedAt = executedAt; return this; }
        public FederatedResult build() { return target; }
    }
}
