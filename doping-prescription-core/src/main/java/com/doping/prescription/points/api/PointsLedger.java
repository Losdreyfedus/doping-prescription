package com.doping.prescription.points.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The points rules and ledger (docs/algoritma.md §7). Writes join the caller's transaction. */
public interface PointsLedger {

    /** Points a correct answer earns, shown on the card before the learner answers. */
    int pointsAvailable(boolean weakTopic);

    /**
     * Applies an answer to the streak and awards its points once per {@code refId}; a repeated {@code refId} awards
     * nothing.
     *
     * @param refId the quiz attempt
     */
    Award award(UUID userId, String refId, boolean correct, boolean weakTopic);

    /** The learner's points and streak as an award of 0, for answers that earn nothing. */
    Award current(UUID userId);

    Summary summary(UUID userId);

    /**
     * @param pointsAwarded points for this answer, streak bonus included
     * @param totalPoints   the learner's total after it
     * @param bonusAt       streak length that earns the bonus
     */
    record Award(int pointsAwarded, int totalPoints, int streak, int bonusAt) {}

    /**
     * @param today  points earned today, in prescription.timezone
     * @param recent latest ledger entries, newest first
     */
    record Summary(int total, int today, int dailyCap, Streak streak, List<Entry> recent) {

        public Summary {
            recent = List.copyOf(recent);
        }
    }

    /** Consecutive correct answers now and at best. */
    record Streak(int current, int best) {}

    /**
     * @param reason QUIZ_CORRECT, QUIZ_CORRECT_WEAK or STREAK_BONUS
     */
    record Entry(int amount, String reason, Instant createdAt) {}
}
