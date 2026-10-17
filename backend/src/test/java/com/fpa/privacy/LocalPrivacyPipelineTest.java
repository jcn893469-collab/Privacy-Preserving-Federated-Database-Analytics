package com.fpa.privacy;

import com.fpa.localprocessing.ZScoreAnomalyDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalPrivacyPipelineTest {

    private LocalPrivacyPipeline pipeline;

    @BeforeEach
    void setUp() {
        PrivacyAccountant accountant = new PrivacyAccountant();
        GaussianMechanism gaussianMechanism = new GaussianMechanism(accountant);
        ZScoreAnomalyDetector detector = new ZScoreAnomalyDetector();
        pipeline = new LocalPrivacyPipeline(gaussianMechanism, detector);
    }

    @Test
    void testEndToEndPipelineProducesProtectedRelease() {
        List<Double> raw = List.of(25.0, 30.0, 28.0, 45.0, 100.0, 29.0, 31.0);
        double lower = 20.0;
        double upper = 80.0;
        double epsilon = 1.0;
        double delta = 1e-5;

        LocalPrivacyPipeline.ProtectedLocalRelease release = pipeline.run(raw, lower, upper, epsilon, delta);

        assertNotNull(release);
        assertEquals(7, release.localStatistics().getCount());
        assertEquals(60.0, release.sensitivity(), 1e-6); // 80 - 20
        assertTrue(release.noiseSigma() > 0);
        assertTrue(release.noiseScale() > 0);
        assertTrue(Double.isFinite(release.noisySum()));
        assertTrue(release.rdpEpsilonCost() > 0);
    }

    @Test
    void testEmptyListThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            pipeline.run(List.of(), 0.0, 10.0, 1.0, 1e-5));
    }
}
