package com.fpa.localprocessing;

import org.springframework.stereotype.Component;

/**
 * Univariate z-score outlier detector. Scores are computed from the local
 * sample mean and sample standard deviation; nothing is random or hardcoded.
 */
@Component
public class ZScoreAnomalyDetector {

    private static final double DEFAULT_THRESHOLD = 3.0;

    public AnomalyDetectionResult detect(double[] values, LocalStatistics stats) {
        return detect(values, stats, DEFAULT_THRESHOLD);
    }

    public AnomalyDetectionResult detect(double[] values, LocalStatistics stats, double threshold) {
        if (stats.getStandardDeviation() == 0.0) {
            return new AnomalyDetectionResult(
                0, 0.0, threshold, 0.0, 0.0,
                "All clipped values are identical; z-score outliers cannot exist.");
        }

        int anomalies = 0;
        double scoreSum = 0.0;
        double maxScore = 0.0;
        for (double v : values) {
            double z = Math.abs((v - stats.getMean()) / stats.getStandardDeviation());
            if (z > threshold) {
                anomalies++;
                scoreSum += z;
                if (z > maxScore) {
                    maxScore = z;
                }
            }
        }

        double rate = (double) anomalies / values.length;
        double meanScore = anomalies == 0 ? 0.0 : scoreSum / anomalies;
        String summary = anomalies == 0
            ? "No values exceeded |z| > " + threshold + "."
            : anomalies + " of " + values.length
                + " records exceeded |z| > " + threshold
                + " (mean |z| of anomalies = " + round4(meanScore)
                + ", max |z| = " + round4(maxScore) + ").";

        return new AnomalyDetectionResult(anomalies, rate, threshold, meanScore, maxScore, summary);
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
