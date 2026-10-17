package com.fpa.api.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FederatedResultResponse {

    private int participatingOrganizations;
    private int contributionCount;
    private int protectedTotalCount;
    private double protectedAggregateSum;
    private double globalProtectedMean;
    private String mechanism;
    private String privacySummary;
    private List<String> participatingOrganizationNames = new ArrayList<>();

    public FederatedResultResponse() {
    }

    public int getParticipatingOrganizations() { return participatingOrganizations; }
    public void setParticipatingOrganizations(int participatingOrganizations) { this.participatingOrganizations = participatingOrganizations; }
    public int getContributionCount() { return contributionCount; }
    public void setContributionCount(int contributionCount) { this.contributionCount = contributionCount; }
    public int getProtectedTotalCount() { return protectedTotalCount; }
    public void setProtectedTotalCount(int protectedTotalCount) { this.protectedTotalCount = protectedTotalCount; }
    public double getProtectedAggregateSum() { return protectedAggregateSum; }
    public void setProtectedAggregateSum(double protectedAggregateSum) { this.protectedAggregateSum = protectedAggregateSum; }
    public double getGlobalProtectedMean() { return globalProtectedMean; }
    public void setGlobalProtectedMean(double globalProtectedMean) { this.globalProtectedMean = globalProtectedMean; }
    public String getMechanism() { return mechanism; }
    public void setMechanism(String mechanism) { this.mechanism = mechanism; }
    public String getPrivacySummary() { return privacySummary; }
    public void setPrivacySummary(String privacySummary) { this.privacySummary = privacySummary; }
    public List<String> getParticipatingOrganizationNames() { return participatingOrganizationNames; }
    public void setParticipatingOrganizationNames(List<String> participatingOrganizationNames) {
        this.participatingOrganizationNames = participatingOrganizationNames;
    }
}
