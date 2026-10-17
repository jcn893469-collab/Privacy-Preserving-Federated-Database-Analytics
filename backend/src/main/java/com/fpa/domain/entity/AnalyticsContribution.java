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

/**
 * Privacy-preserving aggregate only. There is no field for raw records.
 */
@Entity
@Table(name = "analytics_contribution")
public class AnalyticsContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "database_id", nullable = false)
    private FederatedDatabase database;

    @Column(name = "record_count", nullable = false)
    private Integer recordCount;

    @Column(name = "noisy_sum", nullable = false, precision = 18, scale = 4)
    private BigDecimal noisySum;

    @Column(name = "epsilon", nullable = false, precision = 10, scale = 4)
    private BigDecimal epsilon;

    @Column(name = "delta_value", nullable = false, precision = 20, scale = 15)
    private BigDecimal deltaValue;

    @Column(name = "mechanism", nullable = false, length = 40)
    private String mechanism;

    @Column(name = "clipping_lower", precision = 18, scale = 4)
    private BigDecimal clippingLower;

    @Column(name = "clipping_upper", precision = 18, scale = 4)
    private BigDecimal clippingUpper;

    @Column(name = "sensitivity", precision = 18, scale = 4)
    private BigDecimal sensitivity;

    @Column(name = "noise_sigma", precision = 18, scale = 8)
    private BigDecimal noiseSigma;

    @Column(name = "noise_scale", precision = 18, scale = 8)
    private BigDecimal noiseScale;

    @Column(name = "rdp_epsilon_cost", precision = 10, scale = 4)
    private BigDecimal rdpEpsilonCost;

    @Column(name = "anomaly_count")
    private Integer anomalyCount;

    @Column(name = "anomaly_rate", precision = 10, scale = 6)
    private BigDecimal anomalyRate;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    public AnalyticsContribution() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FederatedDatabase getDatabase() { return database; }
    public void setDatabase(FederatedDatabase database) { this.database = database; }
    public Integer getRecordCount() { return recordCount; }
    public void setRecordCount(Integer recordCount) { this.recordCount = recordCount; }
    public BigDecimal getNoisySum() { return noisySum; }
    public void setNoisySum(BigDecimal noisySum) { this.noisySum = noisySum; }
    public BigDecimal getEpsilon() { return epsilon; }
    public void setEpsilon(BigDecimal epsilon) { this.epsilon = epsilon; }
    public BigDecimal getDeltaValue() { return deltaValue; }
    public void setDeltaValue(BigDecimal deltaValue) { this.deltaValue = deltaValue; }
    public String getMechanism() { return mechanism; }
    public void setMechanism(String mechanism) { this.mechanism = mechanism; }
    public BigDecimal getClippingLower() { return clippingLower; }
    public void setClippingLower(BigDecimal clippingLower) { this.clippingLower = clippingLower; }
    public BigDecimal getClippingUpper() { return clippingUpper; }
    public void setClippingUpper(BigDecimal clippingUpper) { this.clippingUpper = clippingUpper; }
    public BigDecimal getSensitivity() { return sensitivity; }
    public void setSensitivity(BigDecimal sensitivity) { this.sensitivity = sensitivity; }
    public BigDecimal getNoiseSigma() { return noiseSigma; }
    public void setNoiseSigma(BigDecimal noiseSigma) { this.noiseSigma = noiseSigma; }
    public BigDecimal getNoiseScale() { return noiseScale; }
    public void setNoiseScale(BigDecimal noiseScale) { this.noiseScale = noiseScale; }
    public BigDecimal getRdpEpsilonCost() { return rdpEpsilonCost; }
    public void setRdpEpsilonCost(BigDecimal rdpEpsilonCost) { this.rdpEpsilonCost = rdpEpsilonCost; }
    public Integer getAnomalyCount() { return anomalyCount; }
    public void setAnomalyCount(Integer anomalyCount) { this.anomalyCount = anomalyCount; }
    public BigDecimal getAnomalyRate() { return anomalyRate; }
    public void setAnomalyRate(BigDecimal anomalyRate) { this.anomalyRate = anomalyRate; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final AnalyticsContribution target = new AnalyticsContribution();

        public Builder database(FederatedDatabase database) { target.database = database; return this; }
        public Builder recordCount(Integer recordCount) { target.recordCount = recordCount; return this; }
        public Builder noisySum(BigDecimal noisySum) { target.noisySum = noisySum; return this; }
        public Builder epsilon(BigDecimal epsilon) { target.epsilon = epsilon; return this; }
        public Builder deltaValue(BigDecimal deltaValue) { target.deltaValue = deltaValue; return this; }
        public Builder mechanism(String mechanism) { target.mechanism = mechanism; return this; }
        public Builder clippingLower(BigDecimal clippingLower) { target.clippingLower = clippingLower; return this; }
        public Builder clippingUpper(BigDecimal clippingUpper) { target.clippingUpper = clippingUpper; return this; }
        public Builder sensitivity(BigDecimal sensitivity) { target.sensitivity = sensitivity; return this; }
        public Builder noiseSigma(BigDecimal noiseSigma) { target.noiseSigma = noiseSigma; return this; }
        public Builder noiseScale(BigDecimal noiseScale) { target.noiseScale = noiseScale; return this; }
        public Builder rdpEpsilonCost(BigDecimal rdpEpsilonCost) { target.rdpEpsilonCost = rdpEpsilonCost; return this; }
        public Builder anomalyCount(Integer anomalyCount) { target.anomalyCount = anomalyCount; return this; }
        public Builder anomalyRate(BigDecimal anomalyRate) { target.anomalyRate = anomalyRate; return this; }
        public Builder submittedAt(LocalDateTime submittedAt) { target.submittedAt = submittedAt; return this; }
        public AnalyticsContribution build() { return target; }
    }
}
