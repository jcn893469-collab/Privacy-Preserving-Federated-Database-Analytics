package com.fpa.api.dto;

import java.math.BigDecimal;

public class PrivacyStatusResponse {

    private Long databaseId;
    private String organizationName;
    private BigDecimal epsilonBudget;
    private BigDecimal epsilonSpent;
    private BigDecimal epsilonRemaining;
    private BigDecimal deltaBudget;
    private int contributionsSubmitted;
    private String mechanism;

    public PrivacyStatusResponse() {
    }

    public PrivacyStatusResponse(Long databaseId, String organizationName, BigDecimal epsilonBudget,
                                  BigDecimal epsilonSpent, BigDecimal deltaBudget, int contributionsSubmitted) {
        this.databaseId = databaseId;
        this.organizationName = organizationName;
        this.epsilonBudget = epsilonBudget;
        this.epsilonSpent = epsilonSpent;
        this.epsilonRemaining = epsilonBudget.subtract(epsilonSpent);
        this.deltaBudget = deltaBudget;
        this.contributionsSubmitted = contributionsSubmitted;
        this.mechanism = "GAUSSIAN_RDP";
    }

    public Long getDatabaseId() { return databaseId; }
    public void setDatabaseId(Long databaseId) { this.databaseId = databaseId; }
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    public BigDecimal getEpsilonBudget() { return epsilonBudget; }
    public void setEpsilonBudget(BigDecimal epsilonBudget) { this.epsilonBudget = epsilonBudget; }
    public BigDecimal getEpsilonSpent() { return epsilonSpent; }
    public void setEpsilonSpent(BigDecimal epsilonSpent) { this.epsilonSpent = epsilonSpent; }
    public BigDecimal getEpsilonRemaining() { return epsilonRemaining; }
    public void setEpsilonRemaining(BigDecimal epsilonRemaining) { this.epsilonRemaining = epsilonRemaining; }
    public BigDecimal getDeltaBudget() { return deltaBudget; }
    public void setDeltaBudget(BigDecimal deltaBudget) { this.deltaBudget = deltaBudget; }
    public int getContributionsSubmitted() { return contributionsSubmitted; }
    public void setContributionsSubmitted(int contributionsSubmitted) { this.contributionsSubmitted = contributionsSubmitted; }
    public String getMechanism() { return mechanism; }
    public void setMechanism(String mechanism) { this.mechanism = mechanism; }
}