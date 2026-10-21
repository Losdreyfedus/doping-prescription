package com.doping.prescription.learner.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Synchronous signal recording for answers the learner sees the effect of at once (docs/mimari.md §4: "%17 → %27").
 * Joins the caller's transaction.
 */
public interface LearnerSignalRecorder {

    /**
     * Records a micro-question answer once per {@code refId}; a repeated {@code refId} changes nothing.
     *
     * @param refId the quiz attempt, the idempotency key
     */
    MasteryChange recordQuizAnswer(UUID userId, Integer topicId, boolean correct, String refId, Instant answeredAt);

    /** The topic's current mastery as an unchanged {@link MasteryChange}, for answers that produce no signal. */
    MasteryChange unchanged(UUID userId, Integer topicId);

    /**
     * Mastery before (decayed) and after the answer, rounded like the API (3 decimals, docs/algoritma.md §1).
     */
    record MasteryChange(Integer topicId, double before, double after) {}
}
