package com.fpa.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response representing a stored analytics contribution.
 * Contains only aggregate/privacy-metadata information — never raw records,
 * never passwords.
 */
public class ContributionResponse {

    private Long id;
    private Long databaseId;
    private String databaseIdentifier;
    private String organizationName;
    private Integer recordCount;
    private BigDecimal noisySum;
    private BigDecimal epsilon;
    private BigDecimal deltaValue;
    private String mechanism;
    private BigDecimal sensitivity;
    private BigDecimal noiseSigma;
    private Integer anomalyCount;
    private BigDecimal anomalyRate;
    private LocalDateTime submittedAt;

    public ContributionResponse() {
    }

    public ContributionResponse(Long id, Long databaseId, String databaseIdentifier, String organizationName,
                                 Integer recordCount, BigDecimal noisySum, BigDecimal epsilon,
                                 BigDecimal deltaValue, String mechanism, BigDecimal sensitivity,
                                 BigDecimal noiseSigma, Integer anomalyCount, BigDecimal anomalyRate,
                                 LocalDateTime submittedAt) {
        this.id = id;
        this.databaseId = databaseId;
        this.databaseIdentifier = databaseIdentifier;
        this.organizationName = organizationName;
        this.recordCount = recordCount;
        this.noisySum = noisySum;
        this.epsilon = epsilon;
        this.deltaValue = deltaValue;
        this.mechanism = mechanism;
        this.sensitivity = sensitivity;
        this.noiseSigma = noiseSigma;
        this.anomalyCount = anomalyCount;
        this.anomalyRate = anomalyRate;
        this.submittedAt = submittedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDatabaseId() { return databaseId; }
    public void setDatabaseId(Long databaseId) { this.databaseId = databaseId; }
    public String getDatabaseIdentifier() { return databaseIdentifier; }
    public void setDatabaseIdentifier(String databaseIdentifier) { this.databaseIdentifier = databaseIdentifier; }
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
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
    public BigDecimal getSensitivity() { return sensitivity; }
    public void setSensitivity(BigDecimal sensitivity) { this.sensitivity = sensitivity; }
    public BigDecimal getNoiseSigma() { return noiseSigma; }
    public void setNoiseSigma(BigDecimal noiseSigma) { this.noiseSigma = noiseSigma; }
    public Integer getAnomalyCount() { return anomalyCount; }
    public void setAnomalyCount(Integer anomalyCount) { this.anomalyCount = anomalyCount; }
    public BigDecimal getAnomalyRate() { return anomalyRate; }
    public void setAnomalyRate(BigDecimal anomalyRate) { this.anomalyRate = anomalyRate; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}