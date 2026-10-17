package com.fpa.localprocessing;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of validating an organization's raw uploaded CSV, before any
 * numeric value leaves the local validation/preprocessing stage.
 */
public class CsvValidationResult {

    private final List<Double> validValues = new ArrayList<>();
    private final List<String> rejectedRowReasons = new ArrayList<>();
    private int totalRowsSeen = 0;

    public void accept(double value) {
        validValues.add(value);
    }

    public void reject(int rowNumber, String reason) {
        rejectedRowReasons.add("Row " + rowNumber + ": " + reason);
    }

    public void incrementRowsSeen() {
        totalRowsSeen++;
    }

    public List<Double> getValidValues() {
        return validValues;
    }

    public List<String> getRejectedRowReasons() {
        return rejectedRowReasons;
    }

    public int getTotalRowsSeen() {
        return totalRowsSeen;
    }

    public int getValidRowCount() {
        return validValues.size();
    }

    public int getRejectedRowCount() {
        return rejectedRowReasons.size();
    }
}