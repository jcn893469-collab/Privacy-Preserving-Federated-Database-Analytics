package com.fpa.service;

import com.fpa.api.dto.FederatedResultResponse;
import com.fpa.domain.entity.AnalyticsContribution;
import com.fpa.domain.entity.FederatedResult;
import com.fpa.domain.entity.FederatedRunParticipant;
import com.fpa.domain.entity.User;
import com.fpa.domain.repository.AnalyticsContributionRepository;
import com.fpa.domain.repository.FederatedResultRepository;
import com.fpa.security.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FederatedAggregationService {

    private final AnalyticsContributionRepository contributionRepository;
    private final FederatedResultRepository federatedResultRepository;
    private final CurrentUserService currentUserService;

    public FederatedAggregationService(AnalyticsContributionRepository contributionRepository,
                                       FederatedResultRepository federatedResultRepository,
                                       CurrentUserService currentUserService) {
        this.contributionRepository = contributionRepository;
        this.federatedResultRepository = federatedResultRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public FederatedResult executeAndPersistFederatedRun(String targetMetric) {
        User executor = currentUserService.requireUser();
        List<AnalyticsContribution> contributions = contributionRepository.findAll();

        int totalCount = 0;
        BigDecimal totalSum = BigDecimal.ZERO;
        for (AnalyticsContribution c : contributions) {
            totalCount += c.getRecordCount();
            totalSum = totalSum.add(c.getNoisySum());
        }

        List<String> orgNames = contributions.stream()
                .map(c -> c.getDatabase().getOrganizationName())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        BigDecimal globalMean = totalCount > 0
                ? totalSum.divide(BigDecimal.valueOf(totalCount), 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String runIdentifier = "FED-RUN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        FederatedResult run = FederatedResult.builder()
                .runIdentifier(runIdentifier)
                .computationType("FEDERATED_MEAN")
                .targetMetric(targetMetric != null ? targetMetric : "overall")
                .participatingOrgCount(orgNames.size())
                .totalContributionCount(contributions.size())
                .protectedTotalRecords(totalCount)
                .protectedAggregateSum(totalSum.setScale(4, RoundingMode.HALF_UP))
                .globalProtectedMean(globalMean)
                .privacyMechanism("GAUSSIAN + RDP")
                .executedBy(executor)
                .executedAt(LocalDateTime.now())
                .build();

        for (AnalyticsContribution c : contributions) {
            FederatedRunParticipant participant = FederatedRunParticipant.builder()
                    .run(run)
                    .contribution(c)
                    .database(c.getDatabase())
                    .organizationName(c.getDatabase().getOrganizationName())
                    .recordCount(c.getRecordCount())
                    .noisySum(c.getNoisySum())
                    .contributedAt(c.getSubmittedAt())
                    .build();
            run.getParticipants().add(participant);
        }

        return federatedResultRepository.save(run);
    }

    @Transactional(readOnly = true)
    public Page<FederatedResult> getHistoricalRuns(String query, Pageable pageable) {
        return federatedResultRepository.searchRuns(query, pageable);
    }

    @Transactional(readOnly = true)
    public FederatedResult getRunById(Long id) {
        return federatedResultRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Federated run not found with id: " + id));
    }
}
