package com.fpa.service;

import com.fpa.api.dto.ContributionResponse;
import com.fpa.domain.entity.FederatedDatabase;
import com.fpa.privacy.LocalPrivacyPipeline;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Isolated demo helper: synthesizes numeric values in memory, runs the real
 * local Gaussian pipeline, discards the raw values, and stores only the
 * protected aggregate. Not used by the organization CSV path.
 */
@Service
public class SimulationService {

    private final LocalPrivacyPipeline localPrivacyPipeline;
    private final AnalyticsContributionService analyticsContributionService;
    private final FederatedDatabaseService federatedDatabaseService;

    public SimulationService(LocalPrivacyPipeline localPrivacyPipeline,
                              AnalyticsContributionService analyticsContributionService,
                              FederatedDatabaseService federatedDatabaseService) {
        this.localPrivacyPipeline = localPrivacyPipeline;
        this.analyticsContributionService = analyticsContributionService;
        this.federatedDatabaseService = federatedDatabaseService;
    }

    @Transactional
    public ContributionResponse runSynthetic(Long databaseId, int recordCount, double minValue,
                                             double maxValue, double epsilon, double delta) {
        if (recordCount < 4) {
            throw new IllegalArgumentException("recordCount must be at least 4");
        }
        FederatedDatabase database = federatedDatabaseService.require(databaseId);
        List<Double> values = new ArrayList<>(recordCount);
        for (int i = 0; i < recordCount; i++) {
            values.add(ThreadLocalRandom.current().nextDouble(minValue, maxValue));
        }

        var release = localPrivacyPipeline.run(values, minValue, maxValue, epsilon, delta);
        values.clear();

        com.fpa.api.dto.ContributionRequest request = new com.fpa.api.dto.ContributionRequest();
        request.setRecordCount(release.localStatistics().getCount());
        request.setNoisySum(BigDecimal.valueOf(release.noisySum()));
        request.setEpsilon(BigDecimal.valueOf(epsilon));
        request.setDelta(BigDecimal.valueOf(delta));
        request.setClippingLower(BigDecimal.valueOf(minValue));
        request.setClippingUpper(BigDecimal.valueOf(maxValue));
        request.setSensitivity(BigDecimal.valueOf(release.sensitivity()));
        request.setNoiseSigma(BigDecimal.valueOf(release.noiseSigma()));
        request.setNoiseScale(BigDecimal.valueOf(release.noiseScale()));
        request.setAnomalyCount(release.anomalies().getAnomalyCount());
        request.setAnomalyRate(BigDecimal.valueOf(release.anomalies().getAnomalyRate()));
        return analyticsContributionService.submitContributionForDatabase(request, database);
    }
}
