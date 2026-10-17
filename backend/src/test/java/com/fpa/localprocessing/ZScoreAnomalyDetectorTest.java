package com.fpa.localprocessing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZScoreAnomalyDetectorTest {

    private ZScoreAnomalyDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ZScoreAnomalyDetector();
    }

    @Test
    void testDetectWithClearOutlier() {
        // 9 standard values near 50, and 1 extreme outlier at 500
        double[] values = new double[]{49, 50, 51, 48, 52, 50, 49, 51, 50, 500};
        LocalStatistics stats = LocalStatistics.compute(values);

        AnomalyDetectionResult result = detector.detect(values, stats, 2.5);
        assertTrue(result.getAnomalyCount() >= 1);
        assertTrue(result.getAnomalyRate() > 0);
        assertTrue(result.getMaxAnomalyScore() > 2.5);
    }

    @Test
    void testDetectWithIdenticalValues() {
        double[] values = new double[]{42.0, 42.0, 42.0, 42.0};
        LocalStatistics stats = LocalStatistics.compute(values);

        AnomalyDetectionResult result = detector.detect(values, stats, 3.0);
        assertEquals(0, result.getAnomalyCount());
        assertEquals(0.0, result.getAnomalyRate());
    }

    @Test
    void testDetectWithNormalDistribution() {
        double[] values = new double[]{10, 11, 9, 10, 12, 8, 10, 11, 9, 10};
        LocalStatistics stats = LocalStatistics.compute(values);

        AnomalyDetectionResult result = detector.detect(values, stats, 3.0);
        assertEquals(0, result.getAnomalyCount());
    }
}
