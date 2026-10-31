package com.fpa.service;

import com.fpa.api.dto.AdminSystemStatusResponse;
import com.fpa.domain.entity.OrganizationContributionSummaryView;
import com.fpa.domain.entity.User;
import com.fpa.domain.repository.AnalyticsContributionRepository;
import com.fpa.domain.repository.DatasetMetadataRepository;
import com.fpa.domain.repository.FederatedDatabaseRepository;
import com.fpa.domain.repository.OrganizationContributionSummaryRepository;
import com.fpa.domain.repository.UserRepository;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

@Service
public class AdminDashboardService {

    private final FederatedDatabaseRepository federatedDatabaseRepository;
    private final UserRepository userRepository;
    private final AnalyticsContributionRepository analyticsContributionRepository;
    private final DatasetMetadataRepository datasetMetadataRepository;
    private final OrganizationContributionSummaryRepository organizationSummaryRepository;
    private final JdbcTemplate jdbcTemplate;

    public AdminDashboardService(FederatedDatabaseRepository federatedDatabaseRepository,
                                  UserRepository userRepository,
                                  AnalyticsContributionRepository analyticsContributionRepository,
                                  DatasetMetadataRepository datasetMetadataRepository,
                                  OrganizationContributionSummaryRepository organizationSummaryRepository,
                                  JdbcTemplate jdbcTemplate) {
        this.federatedDatabaseRepository = federatedDatabaseRepository;
        this.userRepository = userRepository;
        this.analyticsContributionRepository = analyticsContributionRepository;
        this.datasetMetadataRepository = datasetMetadataRepository;
        this.organizationSummaryRepository = organizationSummaryRepository;
        this.jdbcTemplate = jdbcTemplate;
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

    @Transactional(readOnly = true)
    public String generateOrganizationSummary(Long databaseId) {
        String summary = jdbcTemplate.execute((ConnectionCallback<String>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "CALL sp_generate_organization_analytics_summary(?, NULL)")) {
                statement.setLong(1, databaseId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    return resultSet.getString(1);
                }
            }
        });
        if (summary == null) {
            throw new IllegalArgumentException("Federated database not found with id: " + databaseId);
        }
        return summary;
    }
}
