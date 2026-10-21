package com.doping.prescription.exam.persistence;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

/**
 * Composite key of {@link ExamAnswer}.
 *
 * @param attemptId  the attempt
 * @param questionId the answered question
 */
@Embeddable
public record ExamAnswerId(UUID attemptId, Integer questionId) implements Serializable {}
