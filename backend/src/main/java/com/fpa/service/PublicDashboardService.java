package com.fpa.service;

import com.fpa.api.dto.PublicDashboardDatabaseResponse;
import com.fpa.api.dto.PublicDashboardResponse;
import com.fpa.domain.entity.FederatedDatabase;
import com.fpa.domain.repository.AnalyticsContributionRepository;
import com.fpa.domain.repository.FederatedDatabaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PublicDashboardService {

    private final FederatedDatabaseRepository federatedDatabaseRepository;
    private final AnalyticsContributionRepository analyticsContributionRepository;

    public PublicDashboardService(FederatedDatabaseRepository federatedDatabaseRepository,
                                  AnalyticsContributionRepository analyticsContributionRepository) {
        this.federatedDatabaseRepository = federatedDatabaseRepository;
        this.analyticsContributionRepository = analyticsContributionRepository;
    }

    @Transactional(readOnly = true)
    public PublicDashboardResponse getDashboard() {
        long totalProtectedRecords = analyticsContributionRepository.sumGlobalRecordCount();
        BigDecimal protectedSum = analyticsContributionRepository.sumGlobalNoisySum();
        BigDecimal protectedMean = totalProtectedRecords == 0
            ? null
            : protectedSum.divide(BigDecimal.valueOf(totalProtectedRecords), 6, RoundingMode.HALF_UP);

        List<PublicDashboardDatabaseResponse> databases = federatedDatabaseRepository
            .findAllByOrderByRegisteredAtDesc()
            .stream()
            .map(this::toPublicResponse)
            .toList();

        return new PublicDashboardResponse(
            federatedDatabaseRepository.countDistinctOrganizationNames(),
            federatedDatabaseRepository.countByActiveTrue(),
            analyticsContributionRepository.count(),
            totalProtectedRecords,
            protectedSum,
            protectedMean,
            databases
        );
    }

    private PublicDashboardDatabaseResponse toPublicResponse(FederatedDatabase database) {
        return new PublicDashboardDatabaseResponse(
            database.getOrganizationName(),
            database.getDbIdentifier(),
            database.getOwner().getEmail(),
            database.getPrivacyEpsilonBudget(),
            database.getPrivacyEpsilonSpent(),
            database.isActive()
        );
    }
}
