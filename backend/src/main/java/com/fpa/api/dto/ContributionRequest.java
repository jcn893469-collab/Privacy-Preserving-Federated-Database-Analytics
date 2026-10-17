package com.fpa.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Protected aggregate only. No raw records, no unclipped local sum.
 */
public class ContributionRequest {

    @NotNull
    @Positive
    private Integer recordCount;

    @NotNull
    private BigDecimal noisySum;

    @NotNull
    @Positive
    private BigDecimal epsilon;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    @DecimalMax(value = "1", inclusive = false)
    private BigDecimal delta;

    @NotNull
    private BigDecimal clippingLower;

    @NotNull
    private BigDecimal clippingUpper;

    @NotNull
    @Positive
    private BigDecimal sensitivity;

    @NotNull
    @Positive
    private BigDecimal noiseSigma;

    @NotNull
    @Positive
    private BigDecimal noiseScale;

    private Integer anomalyCount;
    private BigDecimal anomalyRate;

    public ContributionRequest() {
    }

    public Integer getRecordCount() { return recordCount; }
    public void setRecordCount(Integer recordCount) { this.recordCount = recordCount; }
    public BigDecimal getNoisySum() { return noisySum; }
    public void setNoisySum(BigDecimal noisySum) { this.noisySum = noisySum; }
    public BigDecimal getEpsilon() { return epsilon; }
    public void setEpsilon(BigDecimal epsilon) { this.epsilon = epsilon; }
    public BigDecimal getDelta() { return delta; }
    public void setDelta(BigDecimal delta) { this.delta = delta; }
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
    public Integer getAnomalyCount() { return anomalyCount; }
    public void setAnomalyCount(Integer anomalyCount) { this.anomalyCount = anomalyCount; }
    public BigDecimal getAnomalyRate() { return anomalyRate; }
    public void setAnomalyRate(BigDecimal anomalyRate) { this.anomalyRate = anomalyRate; }
}
