package com.fpa.localprocessing;

/**
 * Local (non-private, pre-noise) descriptive statistics computed on an
 * organization's own validated data. These numbers are shown to the
 * organization in ITS OWN dashboard only — they are never sent to the
 * central server. Only the downstream Gaussian-noised, clipped sum leaves
 * the organization (see LocalCsvAnalyticsService).
 */
public class LocalStatistics {

    private final int count;
    private final double mean;
    private final double min;
    private final double max;
    private final double standardDeviation;
    private final double trueSum;

    public LocalStatistics(int count, double mean, double min, double max,
                            double standardDeviation, double trueSum) {
        this.count = count;
        this.mean = mean;
        this.min = min;
        this.max = max;
        this.standardDeviation = standardDeviation;
        this.trueSum = trueSum;
    }

    public static LocalStatistics compute(double[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("Cannot compute statistics on zero values");
        }
        double sum = 0.0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double v : values) {
            sum += v;
            if (v < min) min = v;
            if (v > max) max = v;
        }
        double mean = sum / values.length;

        double sqDiffSum = 0.0;
        for (double v : values) {
            sqDiffSum += (v - mean) * (v - mean);
        }
        double stdDev = values.length > 1 ? Math.sqrt(sqDiffSum / (values.length - 1)) : 0.0;

        return new LocalStatistics(values.length, mean, min, max, stdDev, sum);
    }

    public int getCount() {
        return count;
    }

    public double getMean() {
        return mean;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    public double getTrueSum() {
        return trueSum;
    }
}