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
     * @param pool       WEAK_TOPIC, REVIEW_DUE, EXPLORE, POPULAR or FALLBACK
     * @param maxWatchMs furthest point watched, in real video milliseconds
     */
    record ImpressionView(
            UUID impressionId, UUID userId, Integer contentId, Integer topicId, String pool, int maxWatchMs) {}

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
