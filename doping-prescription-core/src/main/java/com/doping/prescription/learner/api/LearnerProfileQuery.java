package com.doping.prescription.learner.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Read access to a learner's topic profile for other modules (the feed). Values are at full precision with decay
 * applied; callers round only for display (docs/algoritma.md §1).
 */
public interface LearnerProfileQuery {

    LearnerProfile profile(UUID userId);

    /**
     * @param lastExamAttemptId the latest exam attempt the profile has processed; {@code null} before the first exam
     * @param topics            every curriculum topic, including those without signals
     */
    record LearnerProfile(UUID userId, @Nullable UUID lastExamAttemptId, List<TopicProfile> topics) {

        public LearnerProfile {
            topics = List.copyOf(topics);
        }
    }

    /**
     * @param effectiveMastery mastery after decay
     * @param band             single definition of docs/algoritma.md §1
     * @param attempts         mastery-affecting signals; 0 means no data
     * @param lastSignalAt     the latest mastery-affecting signal; {@code null} without data
     * @param lastExam         the topic's result in the latest exam; {@code null} if the topic was not in one
     */
    record TopicProfile(
            Integer topicId,
            double effectiveMastery,
            double confidence,
            Band band,
            Trend trend,
            int attempts,
            @Nullable Instant lastSignalAt,
            @Nullable LastExam lastExam) {}

    /** "3 questions, 0 correct" for the why-this-video template. */
    record LastExam(UUID attemptId, int total, int correct) {}

    /**
     * A topic's mastery band as other modules see it. The thresholds stay inside the learner module, so a WEAK topic on
     * Gelişimim is exactly a topic the feed prescribes (docs/algoritma.md §1).
     */
    enum Band {
        /** No mastery-affecting signal yet. */
        UNKNOWN,
        WEAK,
        MEDIUM,
        STRONG
    }

    /** Direction of the topic's latest mastery-affecting signal (docs/algoritma.md §1, Trend). */
    enum Trend {
        UP,
        DOWN,
        FLAT
    }
}
