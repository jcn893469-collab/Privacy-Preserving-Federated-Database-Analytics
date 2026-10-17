package com.fpa.privacy;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renyi Differential Privacy (RDP) / zCDP-style privacy accountant for the
 * Gaussian mechanism, used to track and compose privacy loss across MULTIPLE
 * contributions/rounds for a single organization — not just a single
 * release. This is what makes the platform's privacy guarantee meaningful
 * over time, rather than resetting the budget on every upload.
 *
 * Mechanics:
 *  - The Gaussian mechanism with noise multiplier {@code sigma} (in units of
 *    the query's sensitivity, i.e. noiseScale = sigma / sensitivity) satisfies,
 *    for every order alpha > 1:
 *        RDP(alpha) = alpha / (2 * noiseScale^2)
 *    (Mironov, "Renyi Differential Privacy", 2017.)
 *  - RDP composes ADDITIVELY across independent releases: the RDP of a
 *    sequence of contributions at a fixed alpha is the sum of each
 *    contribution's RDP(alpha).
 *  - The composed RDP curve is converted back to an (epsilon, delta)-DP
 *    guarantee by minimizing, over the alpha grid, the standard conversion
 *    bound (Balle et al. 2020 / Canonne et al. 2020):
 *        epsilon(delta) = min_alpha [ RDP(alpha) + ln(1/delta) / (alpha - 1) ]
 *
 * State (the composed RDP(alpha) values) is kept per organization/database
 * and is expected to be persisted by the caller (see
 * FederatedDatabase.rdpAccountantState) so the budget survives restarts.
 */
@Component
public class PrivacyAccountant {

    /** A reasonably dense grid of Renyi orders, standard in DP-accounting libraries. */
    private static final double[] ALPHA_GRID = buildAlphaGrid();

    private static double[] buildAlphaGrid() {
        // 1.5 .. 64 in fine steps, plus a few larger orders for tighter bounds
        // at small delta.
        java.util.List<Double> alphas = new java.util.ArrayList<>();
        for (double a = 1.5; a <= 64.0; a += 0.5) {
            alphas.add(a);
        }
        for (double a : new double[]{80, 100, 128, 150, 200, 256}) {
            alphas.add(a);
        }
        double[] arr = new double[alphas.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = alphas.get(i);
        }
        return arr;
    }

    /**
     * RDP(alpha) for a single Gaussian mechanism release with the given
     * noise multiplier (sigma / sensitivity). Larger noiseScale => smaller
     * (better) RDP at every order.
     */
    public double rdpOfSingleRelease(double alpha, double noiseScale) {
        if (noiseScale <= 0) {
            throw new IllegalArgumentException("noiseScale must be > 0");
        }
        return alpha / (2.0 * noiseScale * noiseScale);
    }

    /**
     * Composes one new Gaussian release (given as noiseScale = sigma /
     * sensitivity) into an existing RDP accounting state, additively across
     * the whole alpha grid. Returns the UPDATED state; does not mutate the
     * input map.
     */
    public Map<Double, Double> compose(Map<Double, Double> currentState, double noiseScale) {
        Map<Double, Double> updated = new LinkedHashMap<>();
        for (double alpha : ALPHA_GRID) {
            double existing = currentState.getOrDefault(alpha, 0.0);
            updated.put(alpha, existing + rdpOfSingleRelease(alpha, noiseScale));
        }
        return updated;
    }

    /**
     * Converts a composed RDP curve into the tightest (epsilon)-DP guarantee
     * at the given target delta, minimizing over the alpha grid.
     */
    public double rdpToEpsilon(Map<Double, Double> rdpState, double targetDelta) {
        if (rdpState.isEmpty()) {
            return 0.0;
        }
        if (targetDelta <= 0 || targetDelta >= 1) {
            throw new IllegalArgumentException("targetDelta must be in (0, 1)");
        }
        double best = Double.POSITIVE_INFINITY;
        for (Map.Entry<Double, Double> entry : rdpState.entrySet()) {
            double alpha = entry.getKey();
            double rdp = entry.getValue();
            if (alpha <= 1.0) {
                continue;
            }
            double candidate = rdp + (Math.log(1.0 / targetDelta) / (alpha - 1.0));
            if (candidate < best) {
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Throws {@link PrivacyBudgetExceededException} if composing the
     * proposed release into the current state would push the organization's
     * total (epsilon, delta)-DP cost past {@code epsilonBudget} at
     * {@code deltaBudget}. Returns the new composed state if the release is
     * within budget (the caller is responsible for persisting it).
     */
    public Map<Double, Double> checkAndCompose(Map<Double, Double> currentState,
                                                double noiseScale,
                                                double epsilonBudget,
                                                double deltaBudget) {
        Map<Double, Double> proposed = compose(currentState, noiseScale);
        double resultingEpsilon = rdpToEpsilon(proposed, deltaBudget);

        if (resultingEpsilon > epsilonBudget) {
            throw new PrivacyBudgetExceededException(String.format(
                "This contribution would raise the organization's cumulative privacy loss to "
                    + "epsilon=%.4f at delta=%.2e, exceeding the configured budget of epsilon=%.4f. "
                    + "Increase epsilon per-round noise reduction is not possible without more budget; "
                    + "the organization has exhausted its allowed privacy spend.",
                resultingEpsilon, deltaBudget, epsilonBudget));
        }
        return proposed;
    }

    // ---- Serialization helpers: RDP state is persisted on FederatedDatabase
    // as a compact "alpha1:rdp1,alpha2:rdp2,..." string so accounting
    // survives application restarts. ----

    public String serialize(Map<Double, Double> state) {
        return state.entrySet().stream()
            .map(e -> e.getKey() + ":" + e.getValue())
            .collect(Collectors.joining(","));
    }

    public Map<Double, Double> deserialize(String serialized) {
        Map<Double, Double> state = new LinkedHashMap<>();
        if (serialized == null || serialized.isBlank()) {
            return state;
        }
        for (String pair : serialized.split(",")) {
            String[] parts = pair.split(":");
            if (parts.length == 2) {
                state.put(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
            }
        }
        return state;
    }

    public Map<Double, Double> emptyState() {
        return new LinkedHashMap<>();
    }
}