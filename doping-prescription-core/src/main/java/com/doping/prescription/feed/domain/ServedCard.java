package com.doping.prescription.feed.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A ranked card placed in a feed page.
 *
 * @param contentId     the video
 * @param position      0-based position in the session
 * @param pool          candidate pool it came from
 * @param targetTopicId topic the card addresses, or {@code null} for popular/fallback cards
 * @param score         final ranking score
 * @param breakdown     score components for the debug screen
 * @param reasonText    the why-this-video text shown on the card
 */
public record ServedCard(
        Integer contentId,
        int position,
        PoolType pool,
        Integer targetTopicId,
        BigDecimal score,
        ScoreBreakdown breakdown,
        String reasonText) {

    public ServedCard {
        Objects.requireNonNull(contentId, "contentId");
        Objects.requireNonNull(pool, "pool");
        Objects.requireNonNull(score, "score");
        Objects.requireNonNull(breakdown, "breakdown");
        Objects.requireNonNull(reasonText, "reasonText");
    }
}
