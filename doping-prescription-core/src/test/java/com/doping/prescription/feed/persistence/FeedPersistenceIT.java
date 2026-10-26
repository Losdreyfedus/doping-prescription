package com.doping.prescription.feed.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.feed.domain.InteractionType;
import com.doping.prescription.feed.domain.PoolType;
import com.doping.prescription.feed.domain.ScoreBreakdown;
import com.doping.prescription.feed.domain.ServedCard;
import com.doping.prescription.feed.persistence.FeedQueries.NewInteraction;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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
    private FeedQueries queries;

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
    void theSameClientEventIdOfTwoLearnersIsStoredForBoth() {
        // Given: two learners whose web apps happened to send the same client event id
        UUID other = UUID.randomUUID();
        FeedImpression mine = impressions.saveAndFlush(impression(popularCard()));
        FeedImpression theirs = impressions.saveAndFlush(
                new FeedImpression(UUID.randomUUID(), other, UUID.randomUUID(), popularCard(), "v1", NOW));

        // When
        boolean first = queries.recordInteraction(newInteraction(mine.getId(), learner, "shared-id"));
        boolean second = queries.recordInteraction(newInteraction(theirs.getId(), other, "shared-id"));
        boolean repeated = queries.recordInteraction(newInteraction(mine.getId(), learner, "shared-id"));

        // Then: one learner cannot pre-claim another's id, but a learner's own repeat is still ignored
        assertThat(first).isTrue();
        assertThat(second).isTrue();
        assertThat(repeated).isFalse();
    }

    @Test
    void sessionReadsOnlySeeTheLearnersOwnSession() {
        // Given: another learner's card in a session whose id this learner sends in a forged cursor
        UUID session = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        FeedImpression theirs = impressions.saveAndFlush(
                new FeedImpression(UUID.randomUUID(), other, session, olasilikCard(), "v1", NOW));
        queries.recordInteraction(new NewInteraction(theirs.getId(), other, "skip-1", "SKIP", 900, NOW, NOW));

        // When / Then: the owner sees the session, the forger sees nothing
        assertThat(queries.sessionContent(other, session)).containsExactly(33);
        assertThat(queries.recentTopics(other, session, 2)).containsExactly(9);
        assertThat(queries.skipsPerTopic(other, session)).containsEntry(9, 1);
        assertThat(queries.sessionContent(learner, session)).isEmpty();
        assertThat(queries.recentTopics(learner, session, 2)).isEmpty();
        assertThat(queries.skipsPerTopic(learner, session)).isEmpty();
    }

    @Test
    void theRewatchBlockUsesTheServerReceiveTimeNotTheClientClock() {
        // Given: a completion received now whose client clock claims it happened a month ago
        FeedImpression impression = impressions.saveAndFlush(impression(popularCard()));
        queries.recordInteraction(new NewInteraction(
                impression.getId(),
                learner,
                "late-clock",
                "VIEW_COMPLETE",
                57_200,
                NOW.minus(Duration.ofDays(30)),
                NOW));

        // When
        Set<Integer> recent = queries.completedSince(learner, NOW.minus(Duration.ofDays(14)));

        // Then
        assertThat(recent).containsExactly(1);
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

    private static NewInteraction newInteraction(UUID impressionId, UUID userId, String clientEventId) {
        return new NewInteraction(impressionId, userId, clientEventId, "VIEW_COMPLETE", 57_200, NOW, NOW);
    }

    private static ServedCard olasilikCard() {
        return new ServedCard(
                33,
                0,
                PoolType.WEAK_TOPIC,
                9,
                new BigDecimal("0.4513"),
                new ScoreBreakdown(0.374, 0.5, 0.9, 1.0, 0.83, List.of()),
                "Son denemende Olasılık: 3 soruda 0 doğru");
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
