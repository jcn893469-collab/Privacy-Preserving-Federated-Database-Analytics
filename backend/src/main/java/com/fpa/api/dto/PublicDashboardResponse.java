package com.fpa.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record PublicDashboardResponse(
        long totalOrganizations,
        long activeOrganizations,
        long totalContributions,
        long totalProtectedRecords,
        BigDecimal globalProtectedSum,
        BigDecimal globalProtectedMean,
        List<PublicDashboardDatabaseResponse> databases) {
}
