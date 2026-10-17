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
@Table(name = "privacy_budget_ledger")
public class PrivacyBudgetLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "database_id", nullable = false)
    private FederatedDatabase database;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contribution_id", nullable = false)
    private AnalyticsContribution contribution;

    @Column(name = "pre_epsilon_spent", nullable = false, precision = 10, scale = 4)
    private BigDecimal preEpsilonSpent;

    @Column(name = "round_epsilon_cost", nullable = false, precision = 10, scale = 4)
    private BigDecimal roundEpsilonCost;

    @Column(name = "post_epsilon_spent", nullable = false, precision = 10, scale = 4)
    private BigDecimal postEpsilonSpent;

    @Column(name = "noise_scale", nullable = false, precision = 18, scale = 8)
    private BigDecimal noiseScale;

    @Column(name = "noise_sigma", nullable = false, precision = 18, scale = 8)
    private BigDecimal noiseSigma;

    @Column(name = "evaluated_at", nullable = false, updatable = false)
    private LocalDateTime evaluatedAt = LocalDateTime.now();

    public PrivacyBudgetLedger() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FederatedDatabase getDatabase() { return database; }
    public void setDatabase(FederatedDatabase database) { this.database = database; }
    public AnalyticsContribution getContribution() { return contribution; }
    public void setContribution(AnalyticsContribution contribution) { this.contribution = contribution; }
    public BigDecimal getPreEpsilonSpent() { return preEpsilonSpent; }
    public void setPreEpsilonSpent(BigDecimal preEpsilonSpent) { this.preEpsilonSpent = preEpsilonSpent; }
    public BigDecimal getRoundEpsilonCost() { return roundEpsilonCost; }
    public void setRoundEpsilonCost(BigDecimal roundEpsilonCost) { this.roundEpsilonCost = roundEpsilonCost; }
    public BigDecimal getPostEpsilonSpent() { return postEpsilonSpent; }
    public void setPostEpsilonSpent(BigDecimal postEpsilonSpent) { this.postEpsilonSpent = postEpsilonSpent; }
    public BigDecimal getNoiseScale() { return noiseScale; }
    public void setNoiseScale(BigDecimal noiseScale) { this.noiseScale = noiseScale; }
    public BigDecimal getNoiseSigma() { return noiseSigma; }
    public void setNoiseSigma(BigDecimal noiseSigma) { this.noiseSigma = noiseSigma; }
    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final PrivacyBudgetLedger target = new PrivacyBudgetLedger();

        public Builder database(FederatedDatabase database) { target.database = database; return this; }
        public Builder contribution(AnalyticsContribution contribution) { target.contribution = contribution; return this; }
        public Builder preEpsilonSpent(BigDecimal preEpsilonSpent) { target.preEpsilonSpent = preEpsilonSpent; return this; }
        public Builder roundEpsilonCost(BigDecimal roundEpsilonCost) { target.roundEpsilonCost = roundEpsilonCost; return this; }
        public Builder postEpsilonSpent(BigDecimal postEpsilonSpent) { target.postEpsilonSpent = postEpsilonSpent; return this; }
        public Builder noiseScale(BigDecimal noiseScale) { target.noiseScale = noiseScale; return this; }
        public Builder noiseSigma(BigDecimal noiseSigma) { target.noiseSigma = noiseSigma; return this; }
        public Builder evaluatedAt(LocalDateTime evaluatedAt) { target.evaluatedAt = evaluatedAt; return this; }
        public PrivacyBudgetLedger build() { return target; }
    }
}
