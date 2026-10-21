package com.doping.prescription.learner.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.learner.domain.SignalType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/** Commits real transactions (needed for the locking test) and removes its own rows afterwards. */
@IntegrationTest
class LearnerPersistenceIT {

    private static final Instant NOW = Instant.parse("2026-10-20T07:00:00Z");
    private static final int OLASILIK = 9;

    // Values after three wrong exam answers in docs/algoritma.md §1, before any rounding.
    private static final double MASTERY_AFTER_THREE_WRONG = 0.1715;
    private static final double CONFIDENCE_AFTER_THREE = 1 - Math.exp(-3.0 / 5);

    @Autowired
    private LearnerSignalRepository signals;

    @Autowired
    private TopicMasteryRepository masteries;

    private final UUID learner = UUID.randomUUID();

    @AfterEach
    void removeCreatedRows() {
        signals.deleteAll(signals.findAll().stream()
                .filter(signal -> signal.getUserId().equals(learner))
                .toList());
        masteries.deleteAll(masteries.findAll().stream()
                .filter(mastery -> mastery.getUserId().equals(learner))
                .toList());
    }

    @Test
    void rejectsSecondSignalWithSameIdempotencyKey() {
        // Given
        String key = "exam:" + UUID.randomUUID() + ":104";
        signals.saveAndFlush(signal(key));

        // When / Then
        assertThatThrownBy(() -> signals.saveAndFlush(signal(key))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void storesMasteryAtFullPrecision() {
        // Given
        TopicMastery mastery = new TopicMastery(new TopicMasteryId(learner, OLASILIK), 0.5, NOW);
        mastery.recordSignal(MASTERY_AFTER_THREE_WRONG, CONFIDENCE_AFTER_THREE, false, NOW);
        mastery.recordExamSummary(new LastExamSummary(UUID.randomUUID(), 3, 0), NOW);

        // When
        masteries.saveAndFlush(mastery);

        // Then
        TopicMastery loaded = masteries.findById(mastery.getId()).orElseThrow();
        assertThat(loaded.getMastery()).isEqualTo(MASTERY_AFTER_THREE_WRONG);
        assertThat(loaded.getConfidence()).isEqualTo(CONFIDENCE_AFTER_THREE);
        assertThat(loaded.getAttempts()).isEqualTo(1);
        assertThat(loaded.getLastExamSummary()).hasValueSatisfying(summary -> {
            assertThat(summary.total()).isEqualTo(3);
            assertThat(summary.correct()).isZero();
        });
    }

    @Test
    void concurrentUpdateOfSameMasteryFails() {
        // Given: two signals loaded the same mastery row before either was stored
        TopicMastery mastery =
                masteries.saveAndFlush(new TopicMastery(new TopicMasteryId(learner, OLASILIK), 0.5, NOW));
        TopicMastery firstCopy = masteries.findById(mastery.getId()).orElseThrow();
        TopicMastery secondCopy = masteries.findById(mastery.getId()).orElseThrow();
        firstCopy.recordSignal(0.35, 0.18, false, NOW);
        secondCopy.recordSignal(0.65, 0.18, true, NOW);
        masteries.save(firstCopy);

        // When / Then
        assertThatThrownBy(() -> masteries.save(secondCopy))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void databaseRejectsMasteryAboveOne() {
        // Given
        TopicMastery mastery = new TopicMastery(new TopicMasteryId(learner, OLASILIK), 0.5, NOW);
        mastery.recordSignal(1.2, 0.18, true, NOW);

        // When / Then
        assertThatThrownBy(() -> masteries.saveAndFlush(mastery)).isInstanceOf(DataIntegrityViolationException.class);
    }

    private LearnerSignal signal(String idempotencyKey) {
        return new LearnerSignal(learner, OLASILIK, SignalType.EXAM_WRONG, "attempt", NOW, idempotencyKey);
    }
}
