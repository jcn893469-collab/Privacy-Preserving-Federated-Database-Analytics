package com.fpa.localprocessing;

public class AnomalyDetectionResult {

    private final int anomalyCount;
    private final double anomalyRate;
    private final double thresholdUsed;
    private final double meanAnomalyScore;
    private final double maxAnomalyScore;
    private final String summary;

    public AnomalyDetectionResult(int anomalyCount, double anomalyRate, double thresholdUsed,
                                  double meanAnomalyScore, double maxAnomalyScore, String summary) {
        this.anomalyCount = anomalyCount;
        this.anomalyRate = anomalyRate;
        this.thresholdUsed = thresholdUsed;
        this.meanAnomalyScore = meanAnomalyScore;
        this.maxAnomalyScore = maxAnomalyScore;
        this.summary = summary;
    }

    public int getAnomalyCount() { return anomalyCount; }
    public double getAnomalyRate() { return anomalyRate; }
    public double getThresholdUsed() { return thresholdUsed; }
    public double getMeanAnomalyScore() { return meanAnomalyScore; }
    public double getMaxAnomalyScore() { return maxAnomalyScore; }
    public String getSummary() { return summary; }
}
