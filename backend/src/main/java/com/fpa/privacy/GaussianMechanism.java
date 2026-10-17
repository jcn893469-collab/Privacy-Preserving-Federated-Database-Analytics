package com.fpa.privacy;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;

/**
 * Calibrated Gaussian mechanism for (epsilon, delta)-DP / RDP accounting.
 *
 * Pipeline:
 *   clipped values → known L2 sensitivity Δ → noise ~ N(0, σ²) with
 *   σ = Δ · noiseScale → protected aggregate.
 *
 * noiseScale (σ / Δ) is chosen so that a single release, converted from
 * RDP to (ε, δ)-DP via {@link PrivacyAccountant}, meets the requested
 * (epsilon, delta) pair. This is not Laplace noise and not a random stub.
 */
@Component
public class GaussianMechanism {

    private final PrivacyAccountant privacyAccountant;
    private final SecureRandom random = new SecureRandom();

    public GaussianMechanism(PrivacyAccountant privacyAccountant) {
        this.privacyAccountant = privacyAccountant;
    }

    /**
     * Binary-search the smallest noiseScale such that a single Gaussian
     * release is (epsilon, delta)-DP under the RDP conversion bound.
     */
    public double calibrateNoiseScale(double epsilon, double delta) {
        if (epsilon <= 0) {
            throw new IllegalArgumentException("epsilon must be > 0");
        }
        if (delta <= 0 || delta >= 1) {
            throw new IllegalArgumentException("delta must be in (0, 1)");
        }

        double lo = 0.1;
        double hi = 1000.0;
        for (int i = 0; i < 80; i++) {
            double mid = (lo + hi) / 2.0;
            double achieved = privacyAccountant.rdpToEpsilon(
                privacyAccountant.compose(privacyAccountant.emptyState(), mid), delta);
            if (achieved <= epsilon) {
                hi = mid;
            } else {
                lo = mid;
            }
        }
        return hi;
    }

    public double sigma(double sensitivity, double noiseScale) {
        if (sensitivity < 0) {
            throw new IllegalArgumentException("sensitivity must be >= 0");
        }
        return sensitivity * noiseScale;
    }

    public double sampleNoise(double sigma) {
        if (sigma < 0) {
            throw new IllegalArgumentException("sigma must be >= 0");
        }
        if (sigma == 0) {
            return 0.0;
        }
        return sigma * nextGaussian();
    }

    public double protectSum(double clippedSum, double sensitivity, double noiseScale) {
        return clippedSum + sampleNoise(sigma(sensitivity, noiseScale));
    }

    public double rdpEpsilonCost(double noiseScale, double delta) {
        Map<Double, Double> state = privacyAccountant.compose(privacyAccountant.emptyState(), noiseScale);
        return privacyAccountant.rdpToEpsilon(state, delta);
    }

    private double nextGaussian() {
        // Box–Muller, using SecureRandom uniforms.
        double u1 = random.nextDouble();
        double u2 = random.nextDouble();
        while (u1 <= 1e-12) {
            u1 = random.nextDouble();
        }
        return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
    }
}
