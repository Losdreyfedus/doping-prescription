package com.doping.prescription.feed.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read access to served cards for the quiz and admin modules. */
public interface ImpressionQuery {

    Optional<ImpressionView> impression(UUID impressionId);

    /** The cards of the learner's latest feed session in position order, with their score components. */
    List<DebugCard> lastSession(UUID userId);

    /**
     * @param pool       WEAK_TOPIC, REVIEW_DUE, EXPLORE, POPULAR or FALLBACK; for display and analytics only
     * @param maxWatchMs furthest point watched, in real video milliseconds
     * @param weakTopic  the card came from the WEAK_TOPIC pool, i.e. it prescribes a weak topic (docs/algoritma.md §7:
     *                   a correct answer earns the weak-topic points); use this instead of comparing {@code pool}
     */
    record ImpressionView(
            UUID impressionId,
            UUID userId,
            Integer contentId,
            Integer topicId,
            String pool,
            int maxWatchMs,
            boolean weakTopic) {}

    /**
     * @param appliedRules notes of the diversity rules that moved this or other cards
     */
    record DebugCard(
            UUID impressionId,
            int position,
            Integer contentId,
            Integer topicId,
            String pool,
            BigDecimal score,
            double weakness,
            double examImportance,
            double quality,
            double freshness,
            double difficultyFit,
            List<String> appliedRules,
            String reasonText) {

        public DebugCard {
            appliedRules = List.copyOf(appliedRules);
        }
    }
}
