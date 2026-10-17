package com.fpa.api.dto;

import java.math.BigDecimal;

/**
 * Response returned to an organization after a CSV upload finishes the full
 * local pipeline. Contains local statistics and anomaly info (visible only
 * to the uploading organization, never persisted centrally) plus the
 * resulting privacy-preserving contribution that WAS sent to the central
 * server.
 */
public class CsvUploadResponse {

    private int totalRowsSeen;
    private int validRowCount;
    private int rejectedRowCount;

    private double localMean;
    private double localMin;
    private double localMax;
    private double localStandardDeviation;

    private int anomalyCount;
    private double anomalyRate;

    private ContributionResponse contribution;
    private BigDecimal organizationEpsilonSpent;
    private BigDecimal organizationEpsilonBudget;

    public int getTotalRowsSeen() { return totalRowsSeen; }
    public void setTotalRowsSeen(int totalRowsSeen) { this.totalRowsSeen = totalRowsSeen; }
    public int getValidRowCount() { return validRowCount; }
    public void setValidRowCount(int validRowCount) { this.validRowCount = validRowCount; }
    public int getRejectedRowCount() { return rejectedRowCount; }
    public void setRejectedRowCount(int rejectedRowCount) { this.rejectedRowCount = rejectedRowCount; }
    public double getLocalMean() { return localMean; }
    public void setLocalMean(double localMean) { this.localMean = localMean; }
    public double getLocalMin() { return localMin; }
    public void setLocalMin(double localMin) { this.localMin = localMin; }
    public double getLocalMax() { return localMax; }
    public void setLocalMax(double localMax) { this.localMax = localMax; }
    public double getLocalStandardDeviation() { return localStandardDeviation; }
    public void setLocalStandardDeviation(double localStandardDeviation) { this.localStandardDeviation = localStandardDeviation; }
    public int getAnomalyCount() { return anomalyCount; }
    public void setAnomalyCount(int anomalyCount) { this.anomalyCount = anomalyCount; }
    public double getAnomalyRate() { return anomalyRate; }
    public void setAnomalyRate(double anomalyRate) { this.anomalyRate = anomalyRate; }
    public ContributionResponse getContribution() { return contribution; }
    public void setContribution(ContributionResponse contribution) { this.contribution = contribution; }
    public BigDecimal getOrganizationEpsilonSpent() { return organizationEpsilonSpent; }
    public void setOrganizationEpsilonSpent(BigDecimal organizationEpsilonSpent) { this.organizationEpsilonSpent = organizationEpsilonSpent; }
    public BigDecimal getOrganizationEpsilonBudget() { return organizationEpsilonBudget; }
    public void setOrganizationEpsilonBudget(BigDecimal organizationEpsilonBudget) { this.organizationEpsilonBudget = organizationEpsilonBudget; }
}