package com.doping.prescription.points.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.points.domain.PointsReason;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/** Commits real transactions (needed for the locking test) and removes its own rows afterwards. */
@IntegrationTest
class PointsPersistenceIT {

    // A fixed clock far from now: the ledger timestamp must come from the application, not from the database.
    private static final Instant AWARDED_AT = Instant.parse("2026-01-05T09:00:00Z");

    @Autowired
    private PointsLedgerRepository ledger;

    @Autowired
    private PointsStreakRepository streaks;

    private final UUID learner = UUID.randomUUID();

    @AfterEach
    void removeCreatedRows() {
        ledger.deleteAll(ledger.findAll().stream()
                .filter(entry -> entry.getUserId().equals(learner))
                .toList());
        streaks.deleteById(learner);
    }

    @Test
    void storesAwardWithApplicationTimestamp() {
        // When
        PointsLedgerEntry entry = ledger.saveAndFlush(
                new PointsLedgerEntry(learner, 15, PointsReason.QUIZ_CORRECT_WEAK, ref(), AWARDED_AT));

        // Then
        assertThat(ledger.findById(entry.getId()).orElseThrow().getAwardedAt()).isEqualTo(AWARDED_AT);
    }

    @Test
    void rejectsSecondAwardWithSameRefId() {
        // Given
        String refId = ref();
        ledger.saveAndFlush(new PointsLedgerEntry(learner, 10, PointsReason.QUIZ_CORRECT, refId, AWARDED_AT));

        // When / Then
        assertThatThrownBy(() -> ledger.saveAndFlush(
                        new PointsLedgerEntry(learner, 10, PointsReason.QUIZ_CORRECT, refId, AWARDED_AT)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void concurrentStreakUpdateFails() {
        // Given: two answers loaded the same streak before either was stored
        streaks.saveAndFlush(new PointsStreak(learner, AWARDED_AT));
        PointsStreak firstCopy = streaks.findById(learner).orElseThrow();
        PointsStreak secondCopy = streaks.findById(learner).orElseThrow();
        firstCopy.apply(ref(), true, AWARDED_AT);
        secondCopy.apply(ref(), true, AWARDED_AT);
        streaks.save(firstCopy);

        // When / Then
        assertThatThrownBy(() -> streaks.save(secondCopy)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    private static String ref() {
        return "quiz:" + UUID.randomUUID();
    }
}
