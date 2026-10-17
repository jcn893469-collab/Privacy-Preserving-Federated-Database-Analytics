package com.fpa.service;

import com.fpa.api.dto.AdminSystemStatusResponse;
import com.fpa.domain.entity.OrganizationContributionSummaryView;
import com.fpa.domain.entity.User;
import com.fpa.domain.repository.AnalyticsContributionRepository;
import com.fpa.domain.repository.DatasetMetadataRepository;
import com.fpa.domain.repository.FederatedDatabaseRepository;
import com.fpa.domain.repository.OrganizationContributionSummaryRepository;
import com.fpa.domain.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AdminDashboardService {

    private final FederatedDatabaseRepository federatedDatabaseRepository;
    private final UserRepository userRepository;
    private final AnalyticsContributionRepository analyticsContributionRepository;
    private final DatasetMetadataRepository datasetMetadataRepository;
    private final OrganizationContributionSummaryRepository organizationSummaryRepository;

    public AdminDashboardService(FederatedDatabaseRepository federatedDatabaseRepository,
                                  UserRepository userRepository,
                                  AnalyticsContributionRepository analyticsContributionRepository,
                                  DatasetMetadataRepository datasetMetadataRepository,
                                  OrganizationContributionSummaryRepository organizationSummaryRepository) {
        this.federatedDatabaseRepository = federatedDatabaseRepository;
        this.userRepository = userRepository;
        this.analyticsContributionRepository = analyticsContributionRepository;
        this.datasetMetadataRepository = datasetMetadataRepository;
        this.organizationSummaryRepository = organizationSummaryRepository;
    }

    @Transactional(readOnly = true)
    public AdminSystemStatusResponse status() {
        AdminSystemStatusResponse resp = new AdminSystemStatusResponse();
        resp.setTotalOrganizations(federatedDatabaseRepository.count());
        resp.setActiveOrganizations(federatedDatabaseRepository.countByActiveTrue());
        resp.setTotalUsers(userRepository.count());
        resp.setTotalContributions(analyticsContributionRepository.count());
        resp.setTotalDataOwners(userRepository.countByRole(User.Role.DATA_OWNER));

        long totalRecords = analyticsContributionRepository.sumGlobalRecordCount();
        resp.setTotalProtectedRecords(totalRecords);

        BigDecimal globalSum = analyticsContributionRepository.sumGlobalNoisySum();
        resp.setGlobalNoisySum(globalSum);

        double mean = totalRecords > 0
                ? globalSum.divide(BigDecimal.valueOf(totalRecords), 6, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        resp.setGlobalProtectedMean(mean);

        BigDecimal privacySpent = federatedDatabaseRepository.sumTotalPrivacyEpsilonSpent();
        resp.setTotalPrivacySpent(privacySpent);

        Double avgRecords = analyticsContributionRepository.avgGlobalRecordCount();
        resp.setAvgRecordsPerContribution(avgRecords != null ? avgRecords : 0.0);

        Double avgAnomaly = analyticsContributionRepository.avgGlobalAnomalyRate();
        resp.setAvgAnomalyRate(avgAnomaly != null ? avgAnomaly : 0.0);

        Integer minRecords = analyticsContributionRepository.minGlobalRecordCount();
        resp.setMinRecords(minRecords != null ? minRecords : 0);

        Integer maxRecords = analyticsContributionRepository.maxGlobalRecordCount();
        resp.setMaxRecords(maxRecords != null ? maxRecords : 0);

        resp.setTotalDatasets(datasetMetadataRepository.count());
        return resp;
    }

    @Transactional(readOnly = true)
    public List<OrganizationContributionSummaryView> getOrganizationSummaries() {
        return organizationSummaryRepository.findAllByOrderByOrganizationNameAsc();
    }

    @Transactional(readOnly = true)
    public Page<OrganizationContributionSummaryView> searchOrganizationSummaries(String query, Pageable pageable) {
        return organizationSummaryRepository.searchView(query, pageable);
    }
}
