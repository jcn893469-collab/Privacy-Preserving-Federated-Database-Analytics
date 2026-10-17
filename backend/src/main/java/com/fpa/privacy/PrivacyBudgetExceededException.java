package com.fpa.privacy;

/**
 * Thrown when a proposed contribution would push an organization's
 * cumulative privacy loss (epsilon, at its configured delta) past its
 * configured privacy budget. The contribution is rejected outright rather
 * than silently allowed — an organization cannot spend more privacy budget
 * than it has been allotted.
 */
public class PrivacyBudgetExceededException extends RuntimeException {

    public PrivacyBudgetExceededException(String message) {
        super(message);
    }
}