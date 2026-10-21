package com.doping.prescription.points.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PointsStreakTest {

    private static final Instant NOW = Instant.parse("2026-10-20T07:00:00Z");

    private final PointsStreak streak = new PointsStreak(UUID.randomUUID(), NOW);

    @Test
    void correctAnswersExtendStreakAndWrongAnswerResetsIt() {
        // When
        streak.apply("quiz:1", true, NOW);
        streak.apply("quiz:2", true, NOW);
        streak.apply("quiz:3", false, NOW);

        // Then
        assertThat(streak.getCurrent()).isZero();
        assertThat(streak.getBest()).isEqualTo(2);
    }

    @Test
    void applyingSameAnswerTwiceCountsOnce() {
        // When
        streak.apply("quiz:1", true, NOW);
        streak.apply("quiz:1", true, NOW);

        // Then
        assertThat(streak.getCurrent()).isEqualTo(1);
    }
}
