package com.doping.prescription.learner.persistence;

import java.util.Objects;
import java.util.UUID;

/**
 * A learner's result on one topic in their latest exam, e.g. 3 questions, 0 correct.
 *
 * @param attemptId the exam attempt
 * @param total     questions of this topic in the exam
 * @param correct   correctly answered questions of this topic
 */
public record LastExamSummary(UUID attemptId, int total, int correct) {

    public LastExamSummary {
        Objects.requireNonNull(attemptId, "attemptId");
        if (correct < 0 || correct > total) {
            throw new IllegalArgumentException("correct must be between 0 and total");
        }
    }
}
