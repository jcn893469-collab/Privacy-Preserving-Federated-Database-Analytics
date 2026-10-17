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
@Table(name = "federated_run_participant")
public class FederatedRunParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private FederatedResult run;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contribution_id", nullable = false)
    private AnalyticsContribution contribution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "database_id", nullable = false)
    private FederatedDatabase database;

    @Column(name = "organization_name", nullable = false, length = 150)
    private String organizationName;

    @Column(name = "record_count", nullable = false)
    private Integer recordCount;

    @Column(name = "noisy_sum", nullable = false, precision = 18, scale = 4)
    private BigDecimal noisySum;

    @Column(name = "contributed_at", nullable = false)
    private LocalDateTime contributedAt;

    public FederatedRunParticipant() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FederatedResult getRun() { return run; }
    public void setRun(FederatedResult run) { this.run = run; }
    public AnalyticsContribution getContribution() { return contribution; }
    public void setContribution(AnalyticsContribution contribution) { this.contribution = contribution; }
    public FederatedDatabase getDatabase() { return database; }
    public void setDatabase(FederatedDatabase database) { this.database = database; }
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    public Integer getRecordCount() { return recordCount; }
    public void setRecordCount(Integer recordCount) { this.recordCount = recordCount; }
    public BigDecimal getNoisySum() { return noisySum; }
    public void setNoisySum(BigDecimal noisySum) { this.noisySum = noisySum; }
    public LocalDateTime getContributedAt() { return contributedAt; }
    public void setContributedAt(LocalDateTime contributedAt) { this.contributedAt = contributedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final FederatedRunParticipant target = new FederatedRunParticipant();

        public Builder run(FederatedResult run) { target.run = run; return this; }
        public Builder contribution(AnalyticsContribution contribution) { target.contribution = contribution; return this; }
        public Builder database(FederatedDatabase database) { target.database = database; return this; }
        public Builder organizationName(String organizationName) { target.organizationName = organizationName; return this; }
        public Builder recordCount(Integer recordCount) { target.recordCount = recordCount; return this; }
        public Builder noisySum(BigDecimal noisySum) { target.noisySum = noisySum; return this; }
        public Builder contributedAt(LocalDateTime contributedAt) { target.contributedAt = contributedAt; return this; }
        public FederatedRunParticipant build() { return target; }
    }
}
