package com.doping.prescription.feed.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.feed.domain.InteractionType;
import com.doping.prescription.feed.domain.PoolType;
import com.doping.prescription.feed.domain.ScoreBreakdown;
import com.doping.prescription.feed.domain.ServedCard;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class FeedPersistenceIT {

    private static final Instant NOW = Instant.parse("2026-10-20T07:05:00Z");

    @Autowired
    private FeedImpressionRepository impressions;

    @Autowired
    private FeedInteractionRepository interactions;

    @Autowired
    private EntityManager entityManager;

    private final UUID learner = UUID.randomUUID();

    @Test
    void storesImpressionWithScoreBreakdown() {
        // Given
        ScoreBreakdown breakdown =
                new ScoreBreakdown(0.374, 0.5, 0.9, 1.0, 0.83, List.of("same topic 3rd in a row: deferred"));
        ServedCard card = new ServedCard(
                33,
                0,
                PoolType.WEAK_TOPIC,
                9,
                new BigDecimal("0.4513"),
                breakdown,
                "Son denemende Olasılık: 3 soruda 0 doğru");
        FeedImpression impression = impressions.save(impression(card));

        // When
        entityManager.flush();
        entityManager.clear();

        // Then
        ServedCard loaded =
                impressions.findById(impression.getId()).orElseThrow().getCard();
        assertThat(loaded).isEqualTo(card);
    }

    @Test
    void rejectsSecondInteractionWithSameClientEventId() {
        // Given
        FeedImpression impression = impressions.saveAndFlush(impression(popularCard()));
        interactions.saveAndFlush(interaction(impression.getId(), "client-1"));

        // When / Then
        assertThatThrownBy(() -> interactions.saveAndFlush(interaction(impression.getId(), "client-1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsInteractionForUnknownImpression() {
        assertThatThrownBy(() -> interactions.saveAndFlush(interaction(UUID.randomUUID(), "client-2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private FeedImpression impression(ServedCard card) {
        return new FeedImpression(UUID.randomUUID(), learner, UUID.randomUUID(), card, "v1", NOW);
    }

    private FeedInteraction interaction(UUID impressionId, String clientEventId) {
        return new FeedInteraction(impressionId, learner, clientEventId, InteractionType.VIEW_COMPLETE, 57_200, NOW);
    }

    private static ServedCard popularCard() {
        return new ServedCard(
                1,
                5,
                PoolType.POPULAR,
                null,
                new BigDecimal("0.2100"),
                new ScoreBreakdown(0, 0.9, 0.95, 1.0, 0.7, List.of()),
                "Bu hafta en çok izlenenlerden");
    }
}
