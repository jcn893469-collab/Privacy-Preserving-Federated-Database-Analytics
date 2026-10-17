package com.fpa.api.dto;

import java.math.BigDecimal;

public class AdminSystemStatusResponse {

    private long totalOrganizations;
    private long activeOrganizations;
    private long totalUsers;
    private long totalContributions;
    private long totalDataOwners;
    private long totalProtectedRecords;
    private BigDecimal globalNoisySum;
    private double globalProtectedMean;
    private BigDecimal totalPrivacySpent;
    private double avgRecordsPerContribution;
    private double avgAnomalyRate;
    private int minRecords;
    private int maxRecords;
    private long totalDatasets;
    private String status;

    public AdminSystemStatusResponse() {
        this.status = "UP";
    }

    public long getTotalOrganizations() { return totalOrganizations; }
    public void setTotalOrganizations(long totalOrganizations) { this.totalOrganizations = totalOrganizations; }
    public long getActiveOrganizations() { return activeOrganizations; }
    public void setActiveOrganizations(long activeOrganizations) { this.activeOrganizations = activeOrganizations; }
    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
    public long getTotalContributions() { return totalContributions; }
    public void setTotalContributions(long totalContributions) { this.totalContributions = totalContributions; }
    public long getTotalDataOwners() { return totalDataOwners; }
    public void setTotalDataOwners(long totalDataOwners) { this.totalDataOwners = totalDataOwners; }
    public long getTotalProtectedRecords() { return totalProtectedRecords; }
    public void setTotalProtectedRecords(long totalProtectedRecords) { this.totalProtectedRecords = totalProtectedRecords; }
    public BigDecimal getGlobalNoisySum() { return globalNoisySum; }
    public void setGlobalNoisySum(BigDecimal globalNoisySum) { this.globalNoisySum = globalNoisySum; }
    public double getGlobalProtectedMean() { return globalProtectedMean; }
    public void setGlobalProtectedMean(double globalProtectedMean) { this.globalProtectedMean = globalProtectedMean; }
    public BigDecimal getTotalPrivacySpent() { return totalPrivacySpent; }
    public void setTotalPrivacySpent(BigDecimal totalPrivacySpent) { this.totalPrivacySpent = totalPrivacySpent; }
    public double getAvgRecordsPerContribution() { return avgRecordsPerContribution; }
    public void setAvgRecordsPerContribution(double avgRecordsPerContribution) { this.avgRecordsPerContribution = avgRecordsPerContribution; }
    public double getAvgAnomalyRate() { return avgAnomalyRate; }
    public void setAvgAnomalyRate(double avgAnomalyRate) { this.avgAnomalyRate = avgAnomalyRate; }
    public int getMinRecords() { return minRecords; }
    public void setMinRecords(int minRecords) { this.minRecords = minRecords; }
    public int getMaxRecords() { return maxRecords; }
    public void setMaxRecords(int maxRecords) { this.maxRecords = maxRecords; }
    public long getTotalDatasets() { return totalDatasets; }
    public void setTotalDatasets(long totalDatasets) { this.totalDatasets = totalDatasets; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}