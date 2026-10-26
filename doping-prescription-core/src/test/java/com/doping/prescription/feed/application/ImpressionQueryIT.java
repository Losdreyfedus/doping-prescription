package com.doping.prescription.feed.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.feed.api.ImpressionQuery;
import com.doping.prescription.feed.api.ImpressionQuery.ImpressionView;
import com.doping.prescription.feed.domain.PoolType;
import com.doping.prescription.feed.domain.ScoreBreakdown;
import com.doping.prescription.feed.domain.ServedCard;
import com.doping.prescription.feed.persistence.FeedImpression;
import com.doping.prescription.feed.persistence.FeedImpressionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** The impression view other modules read; the quiz decides weak-topic points from it (docs/algoritma.md §7). */
@IntegrationTest
@Transactional
class ImpressionQueryIT {

    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");

    @Autowired
    private ImpressionQuery query;

    @Autowired
    private FeedImpressionRepository impressions;

    @ParameterizedTest
    @EnumSource(PoolType.class)
    void onlyAWeakTopicCardIsMarkedAsWeakTopic(PoolType pool) {
        // Given
        FeedImpression impression = impressions.saveAndFlush(new FeedImpression(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new ServedCard(
                        33,
                        0,
                        pool,
                        9,
                        new BigDecimal("0.4000"),
                        new ScoreBreakdown(0, 0, 0, 0, 0, List.of()),
                        "Dopingle'da yeni"),
                "v1",
                NOW));

        // When
        ImpressionView view = query.impression(impression.getId()).orElseThrow();

        // Then
        assertThat(view.pool()).isEqualTo(pool.name());
        assertThat(view.weakTopic()).isEqualTo(pool == PoolType.WEAK_TOPIC);
        assertThat(view.maxWatchMs()).isZero();
    }
}
