package com.doping.prescription.learner.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.content.seed.ContentDemoDataLoader;
import com.doping.prescription.exam.api.ExamResultPublished;
import com.doping.prescription.exam.api.ExamResultPublished.AnswerOutcome;
import com.doping.prescription.learner.api.LearnerProfileQuery;
import com.doping.prescription.learner.api.LearnerProfileQuery.Band;
import com.doping.prescription.learner.api.LearnerProfileQuery.LastExam;
import com.doping.prescription.learner.api.LearnerProfileQuery.LearnerProfile;
import com.doping.prescription.learner.api.LearnerProfileQuery.TopicProfile;
import com.doping.prescription.learner.api.LearnerProfileQuery.Trend;
import com.doping.prescription.learner.domain.MasteryRounding;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Applies exam results straight to the service. This context has no Redis (TestcontainersConfiguration points it at an
 * unreachable port), so every profile read here also proves the PostgreSQL fallback (docs/mimari.md §5).
 */
@IntegrationTest
class MasteryServiceIT {

    private static final int PERKOM = 8;
    private static final int OLASILIK = 9;
    private static final int FONKSIYONLAR = 10;

    @Autowired
    private MasteryService mastery;

    @Autowired
    private LearnerProfileQuery profiles;

    @Autowired
    private ContentDemoDataLoader content;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void seedTopics() {
        content.load();
    }

    @Test
    void examResultUpdatesTheProfileWithTheDocumentedValues() {
        UUID userId = UUID.randomUUID();
        ExamResultPublished result = scenarioResult(userId);

        mastery.applyExamResult(result);

        LearnerProfile profile = profiles.profile(userId);
        assertThat(profile.lastExamAttemptId()).isEqualTo(result.attemptId());
        assertThat(profile.topics()).hasSize(10);
        TopicProfile olasilik = topic(profile, OLASILIK);
        assertThat(MasteryRounding.forApi(olasilik.effectiveMastery())).isEqualByComparingTo("0.172");
        assertThat(MasteryRounding.forApi(olasilik.confidence())).isEqualByComparingTo("0.451");
        assertThat(olasilik.band()).isEqualTo(Band.WEAK);
        assertThat(olasilik.trend()).isEqualTo(Trend.DOWN);
        assertThat(olasilik.lastExam()).isEqualTo(new LastExam(result.attemptId(), 3, 0));
        TopicProfile perkom = topic(profile, PERKOM);
        assertThat(MasteryRounding.forApi(perkom.effectiveMastery())).isEqualByComparingTo("0.494");
        assertThat(MasteryRounding.forApi(perkom.confidence())).isEqualByComparingTo("0.330");
        assertThat(perkom.lastExam()).isEqualTo(new LastExam(result.attemptId(), 2, 1));
        assertThat(topic(profile, FONKSIYONLAR).band()).isEqualTo(Band.UNKNOWN);
    }

    @Test
    void theSameEventTwiceChangesNothing() {
        UUID userId = UUID.randomUUID();
        ExamResultPublished result = scenarioResult(userId);
        mastery.applyExamResult(result);
        LearnerProfile once = profiles.profile(userId);

        mastery.applyExamResult(result);

        LearnerProfile twice = profiles.profile(userId);
        assertThat(topic(twice, OLASILIK).attempts()).isEqualTo(3);
        assertThat(topic(twice, OLASILIK).effectiveMastery())
                .isCloseTo(topic(once, OLASILIK).effectiveMastery(), offset(1e-6));
    }

    @Test
    void onlyAWatchedOrSkippedCardMarksTheTopicServed() {
        // Given
        UUID userId = UUID.randomUUID();
        mastery.applyExamResult(scenarioResult(userId));
        Instant servedAt = Instant.parse("2026-10-22T09:00:00Z");

        // When: starting a video or seeing its question is no serving
        mastery.recordServed(UUID.randomUUID(), userId, OLASILIK, "VIEW_START", servedAt);
        mastery.recordServed(UUID.randomUUID(), userId, OLASILIK, "QUIZ_SHOWN", servedAt);

        // Then
        assertThat(lastServedAt(userId, OLASILIK)).isEmpty();

        // When
        mastery.recordServed(UUID.randomUUID(), userId, OLASILIK, "VIEW_COMPLETE", servedAt);

        // Then
        assertThat(lastServedAt(userId, OLASILIK)).contains(servedAt);
    }

    @Test
    void aRedeliveredInteractionChangesNothing() {
        // Given
        UUID userId = UUID.randomUUID();
        mastery.applyExamResult(scenarioResult(userId));
        UUID eventId = UUID.randomUUID();
        Instant servedAt = Instant.parse("2026-10-22T09:00:00Z");
        mastery.recordServed(eventId, userId, OLASILIK, "SKIP", servedAt);

        // When: the same event again, as Kafka redelivers it (here with a later time to make a change visible)
        mastery.recordServed(eventId, userId, OLASILIK, "SKIP", servedAt.plusSeconds(60));

        // Then
        assertThat(lastServedAt(userId, OLASILIK)).contains(servedAt);
    }

    @Test
    void aLearnerWithoutSignalsHasOnlyUnknownTopics() {
        LearnerProfile profile = profiles.profile(UUID.randomUUID());

        assertThat(profile.lastExamAttemptId()).isNull();
        assertThat(profile.topics())
                .hasSize(10)
                .allSatisfy(topic -> assertThat(topic.band()).isEqualTo(Band.UNKNOWN));
    }

    /** The scenario's results on Olasılık (3 wrong) and Permütasyon-Kombinasyon (1 correct, 1 blank). */
    private static ExamResultPublished scenarioResult(UUID userId) {
        return new ExamResultPublished(
                UUID.randomUUID(),
                ExamResultPublished.SCHEMA_VERSION,
                Instant.now(),
                userId,
                UUID.randomUUID(),
                1,
                List.of(
                        new AnswerOutcome(106, PERKOM, "CORRECT", 30),
                        new AnswerOutcome(107, OLASILIK, "WRONG", 30),
                        new AnswerOutcome(108, PERKOM, "BLANK", null),
                        new AnswerOutcome(109, OLASILIK, "WRONG", 30),
                        new AnswerOutcome(110, OLASILIK, "WRONG", 30)));
    }

    private Optional<Instant> lastServedAt(UUID userId, int topicId) {
        return jdbc.sql(
                        "select last_served_at from learner_topic_mastery where user_id = :userId and topic_id = :topicId")
                .param("userId", userId)
                .param("topicId", topicId)
                .query((row, rowNumber) -> Optional.ofNullable(row.getObject("last_served_at", OffsetDateTime.class))
                        .map(OffsetDateTime::toInstant))
                .single();
    }

    private static TopicProfile topic(LearnerProfile profile, int topicId) {
        return profile.topics().stream()
                .filter(topic -> topic.topicId() == topicId)
                .findFirst()
                .orElseThrow();
    }
}
