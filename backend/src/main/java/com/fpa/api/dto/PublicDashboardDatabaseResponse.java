package com.fpa.api.dto;

import java.math.BigDecimal;

public record PublicDashboardDatabaseResponse(
        String organizationName,
        String dbIdentifier,
        String ownerEmail,
        BigDecimal privacyEpsilonBudget,
        BigDecimal privacyEpsilonSpent,
        boolean active) {
}
