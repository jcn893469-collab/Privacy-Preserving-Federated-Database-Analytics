package com.fpa.privacy;

import com.fpa.localprocessing.AnomalyDetectionResult;
import com.fpa.localprocessing.LocalStatistics;
import com.fpa.localprocessing.ZScoreAnomalyDetector;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Organization-side pipeline that NEVER persists raw values:
 * validate numbers already in memory → clip → local stats → z-score
 * anomalies → Gaussian mechanism → protected sum only.
 */
@Component
public class LocalPrivacyPipeline {

    private final GaussianMechanism gaussianMechanism;
    private final ZScoreAnomalyDetector anomalyDetector;

    public LocalPrivacyPipeline(GaussianMechanism gaussianMechanism, ZScoreAnomalyDetector anomalyDetector) {
        this.gaussianMechanism = gaussianMechanism;
        this.anomalyDetector = anomalyDetector;
    }

    public ProtectedLocalRelease run(List<Double> validValues,
                                     double clipLower,
                                     double clipUpper,
                                     double epsilon,
                                     double delta) {
        if (validValues == null || validValues.isEmpty()) {
            throw new IllegalArgumentException("No valid local values to protect");
        }

        double[] clipped = ClippingUtil.clipAll(validValues, clipLower, clipUpper);
        double sensitivity = ClippingUtil.sumSensitivity(clipLower, clipUpper);
        if (sensitivity <= 0) {
            throw new IllegalArgumentException("Clipping bounds must define a positive sensitivity");
        }

        LocalStatistics stats = LocalStatistics.compute(clipped);
        AnomalyDetectionResult anomalies = anomalyDetector.detect(clipped, stats);

        double noiseScale = gaussianMechanism.calibrateNoiseScale(epsilon, delta);
        double sigma = gaussianMechanism.sigma(sensitivity, noiseScale);
        double noisySum = gaussianMechanism.protectSum(stats.getTrueSum(), sensitivity, noiseScale);
        double rdpCost = gaussianMechanism.rdpEpsilonCost(noiseScale, delta);

        return new ProtectedLocalRelease(
            stats,
            anomalies,
            clipLower,
            clipUpper,
            sensitivity,
            epsilon,
            delta,
            noiseScale,
            sigma,
            noisySum,
            rdpCost
        );
    }

    public record ProtectedLocalRelease(
        LocalStatistics localStatistics,
        AnomalyDetectionResult anomalies,
        double clipLower,
        double clipUpper,
        double sensitivity,
        double epsilon,
        double delta,
        double noiseScale,
        double noiseSigma,
        double noisySum,
        double rdpEpsilonCost
    ) {
    }
}
