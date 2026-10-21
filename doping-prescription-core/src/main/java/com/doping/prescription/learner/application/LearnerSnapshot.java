package com.doping.prescription.learner.application;

import com.doping.prescription.learner.domain.SignalType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A learner's stored profile as it is cached in Redis (docs/mimari.md §5): raw mastery without decay, so a cached copy
 * never goes stale as time passes; decay, bands and trends are derived on every read.
 *
 * @param lastExamAttemptId the latest exam attempt the profile has processed
 * @param topics            only the topics the learner has data for
 */
public record LearnerSnapshot(@Nullable UUID lastExamAttemptId, List<TopicSnapshot> topics) {

    public LearnerSnapshot {
        topics = List.copyOf(topics);
    }

    /**
     * @param lastSignal the latest mastery-affecting signal, for the trend
     * @param lastExam   the topic's result in the latest exam
     */
    public record TopicSnapshot(
            Integer topicId,
            double mastery,
            double confidence,
            int attempts,
            @Nullable Instant lastSignalAt,
            @Nullable SignalType lastSignal,
            @Nullable ExamSummary lastExam) {}

    /** "3 questions, 0 correct" on one topic in one attempt. */
    public record ExamSummary(UUID attemptId, int total, int correct) {}
}
