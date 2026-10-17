package com.fpa.privacy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PrivacyAccountantTest {

    private PrivacyAccountant accountant;

    @BeforeEach
    void setUp() {
        accountant = new PrivacyAccountant();
    }

    @Test
    void testRdpOfSingleRelease() {
        double alpha = 2.0;
        double noiseScale = 1.0;
        double rdp = accountant.rdpOfSingleRelease(alpha, noiseScale);
        assertEquals(1.0, rdp, 1e-6);

        // Larger noise scale must produce smaller RDP (stronger privacy)
        double rdpHighNoise = accountant.rdpOfSingleRelease(alpha, 2.0);
        assertTrue(rdpHighNoise < rdp);
    }

    @Test
    void testComposeIsAdditive() {
        Map<Double, Double> initial = accountant.emptyState();
        Map<Double, Double> round1 = accountant.compose(initial, 1.5);
        Map<Double, Double> round2 = accountant.compose(round1, 1.5);

        double alpha = 4.0;
        double single = accountant.rdpOfSingleRelease(alpha, 1.5);
        assertEquals(single, round1.get(alpha), 1e-6);
        assertEquals(2 * single, round2.get(alpha), 1e-6);
    }

    @Test
    void testRdpToEpsilonConversion() {
        Map<Double, Double> state = accountant.compose(accountant.emptyState(), 2.0);
        double delta = 1e-5;
        double epsilon = accountant.rdpToEpsilon(state, delta);

        assertTrue(epsilon > 0);
        assertTrue(Double.isFinite(epsilon));
    }

    @Test
    void testCheckAndComposeBudgetEnforcement() {
        Map<Double, Double> state = accountant.emptyState();
        double noiseScale = 0.5; // low noise -> high epsilon
        double budget = 2.0;
        double delta = 1e-5;

        // Composing many high-privacy-loss releases must eventually trip budget
        Map<Double, Double> current = state;
        assertThrows(PrivacyBudgetExceededException.class, () -> {
            Map<Double, Double> s = current;
            for (int i = 0; i < 20; i++) {
                s = accountant.checkAndCompose(s, noiseScale, budget, delta);
            }
        });
    }

    @Test
    void testSerializationPreservesState() {
        Map<Double, Double> composed = accountant.compose(accountant.emptyState(), 1.8);
        String serialized = accountant.serialize(composed);
        assertNotNull(serialized);
        assertFalse(serialized.isBlank());

        Map<Double, Double> deserialized = accountant.deserialize(serialized);
        assertEquals(composed.size(), deserialized.size());
        for (Double alpha : composed.keySet()) {
            assertEquals(composed.get(alpha), deserialized.get(alpha), 1e-9);
        }
    }
}
