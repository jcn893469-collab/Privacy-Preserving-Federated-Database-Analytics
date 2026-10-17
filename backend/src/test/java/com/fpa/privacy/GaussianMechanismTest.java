package com.fpa.privacy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GaussianMechanismTest {

    private GaussianMechanism gaussianMechanism;

    @BeforeEach
    void setUp() {
        PrivacyAccountant accountant = new PrivacyAccountant();
        gaussianMechanism = new GaussianMechanism(accountant);
    }

    @Test
    void testCalibrateNoiseScale() {
        double epsilon = 1.0;
        double delta = 1e-5;
        double noiseScale = gaussianMechanism.calibrateNoiseScale(epsilon, delta);

        assertTrue(noiseScale > 0);
        // Smaller epsilon (higher privacy) must require higher noise scale
        double noiseScaleHighPrivacy = gaussianMechanism.calibrateNoiseScale(0.1, delta);
        assertTrue(noiseScaleHighPrivacy > noiseScale);
    }

    @Test
    void testSigmaCalculation() {
        double sensitivity = 50.0;
        double noiseScale = 2.0;
        double sigma = gaussianMechanism.sigma(sensitivity, noiseScale);
        assertEquals(100.0, sigma, 1e-6);
    }

    @Test
    void testProtectSumAddsNoise() {
        double trueSum = 1000.0;
        double sensitivity = 20.0;
        double noiseScale = 1.5;

        double noisySum1 = gaussianMechanism.protectSum(trueSum, sensitivity, noiseScale);
        double noisySum2 = gaussianMechanism.protectSum(trueSum, sensitivity, noiseScale);

        assertTrue(Double.isFinite(noisySum1));
        assertTrue(Double.isFinite(noisySum2));
        assertNotEquals(noisySum1, noisySum2, "Two distinct Gaussian noise samples should not be identical");
    }

    @Test
    void testInvalidParametersThrowException() {
        assertThrows(IllegalArgumentException.class, () -> gaussianMechanism.calibrateNoiseScale(-1.0, 1e-5));
        assertThrows(IllegalArgumentException.class, () -> gaussianMechanism.calibrateNoiseScale(1.0, 1.5));
        assertThrows(IllegalArgumentException.class, () -> gaussianMechanism.sigma(-5.0, 1.0));
    }
}
