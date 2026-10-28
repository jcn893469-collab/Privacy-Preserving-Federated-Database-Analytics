package com.fpa.api.dto;

import com.fpa.domain.entity.FederatedResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FederatedRunResponse(
    Long id,
    String runIdentifier,
    String computationType,
    String targetMetric,
    Integer participatingOrgCount,
    Integer totalContributionCount,
    Integer protectedTotalRecords,
    BigDecimal protectedAggregateSum,
    BigDecimal globalProtectedMean,
    String privacyMechanism,
    String executedByEmail,
    LocalDateTime executedAt
) {
    public static FederatedRunResponse from(FederatedResult run) {
        return new FederatedRunResponse(
            run.getId(),
            run.getRunIdentifier(),
            run.getComputationType(),
            run.getTargetMetric(),
            run.getParticipatingOrgCount(),
            run.getTotalContributionCount(),
            run.getProtectedTotalRecords(),
            run.getProtectedAggregateSum(),
            run.getGlobalProtectedMean(),
            run.getPrivacyMechanism(),
            run.getExecutedBy() == null ? "SYSTEM" : run.getExecutedBy().getEmail(),
            run.getExecutedAt()
        );
    }
}
