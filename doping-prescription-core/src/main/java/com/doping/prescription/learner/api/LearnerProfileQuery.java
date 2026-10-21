package com.doping.prescription.learner.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read access to a learner's topic profile for other modules (the feed). Values are at full precision with decay
 * applied; callers round only for display (docs/algoritma.md §1).
 */
public interface LearnerProfileQuery {

    LearnerProfile profile(UUID userId);

    /**
     * @param lastExamAttemptId the latest exam attempt the profile has processed
     * @param topics            every curriculum topic, including those without signals
     */
    record LearnerProfile(UUID userId, Optional<UUID> lastExamAttemptId, List<TopicProfile> topics) {

        public LearnerProfile {
            topics = List.copyOf(topics);
        }
    }

    /**
     * @param effectiveMastery mastery after decay
     * @param band             UNKNOWN, WEAK, MEDIUM or STRONG (single definition, docs/algoritma.md §1)
     * @param trend            UP, DOWN or FLAT
     * @param attempts         mastery-affecting signals; 0 means no data
     * @param lastExam         the topic's result in the latest exam, if any
     */
    record TopicProfile(
            Integer topicId,
            double effectiveMastery,
            double confidence,
            String band,
            String trend,
            int attempts,
            Optional<Instant> lastSignalAt,
            Optional<LastExam> lastExam) {}

    /** "3 questions, 0 correct" for the why-this-video template. */
    record LastExam(UUID attemptId, int total, int correct) {}
}
