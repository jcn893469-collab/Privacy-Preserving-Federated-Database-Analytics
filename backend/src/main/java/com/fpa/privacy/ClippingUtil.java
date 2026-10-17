package com.fpa.privacy;

import java.util.List;

/**
 * Bounds ("clips") raw local values into a fixed range [lower, upper] before
 * any aggregation happens.
 *
 * Clipping is what makes the sensitivity of a later sum/mean query finite
 * and known in advance: once every value is guaranteed to lie in
 * [lower, upper], adding or removing a single record can change the sum by
 * at most (upper - lower), which becomes the L1/L2 sensitivity used to
 * calibrate the Gaussian mechanism's noise.
 *
 * This runs entirely on the organization's side, before any privacy
 * mechanism is applied and long before anything is transmitted.
 */
public final class ClippingUtil {

    private ClippingUtil() {
    }

    public static double clip(double value, double lower, double upper) {
        if (lower > upper) {
            throw new IllegalArgumentException("lower bound cannot exceed upper bound");
        }
        return Math.max(lower, Math.min(upper, value));
    }

    public static double[] clipAll(List<Double> values, double lower, double upper) {
        double[] clipped = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            clipped[i] = clip(values.get(i), lower, upper);
        }
        return clipped;
    }

    /**
     * L2 sensitivity of a bounded SUM query under add/remove-one-record
     * neighboring relation: a single record can move the sum by at most
     * (upper - lower).
     */
    public static double sumSensitivity(double lower, double upper) {
        return upper - lower;
    }
}