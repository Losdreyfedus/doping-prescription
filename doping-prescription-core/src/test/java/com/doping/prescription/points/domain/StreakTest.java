package com.doping.prescription.points.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StreakTest {

    @Test
    void correctAnswersExtendTheStreakAndAWrongOneEndsItButKeepsTheBest() {
        // When
        Streak streak = Streak.NONE.apply("quiz:1", true).apply("quiz:2", true).apply("quiz:3", false);

        // Then
        assertThat(streak).isEqualTo(new Streak(0, 2, "quiz:3"));
    }

    @Test
    void theSameAnswerAppliedTwiceCountsOnce() {
        // Given
        Streak once = Streak.NONE.apply("quiz:1", true);

        // When
        Streak twice = once.apply("quiz:1", true);

        // Then
        assertThat(twice).isSameAs(once);
        assertThat(twice.current()).isEqualTo(1);
    }

    @Test
    void aNewRunBelowTheBestKeepsTheBest() {
        // Given
        Streak streak = new Streak(0, 4, "quiz:9");

        // When
        Streak next = streak.apply("quiz:10", true);

        // Then
        assertThat(next).isEqualTo(new Streak(1, 4, "quiz:10"));
    }

    @Test
    void rejectsACurrentRunLongerThanTheBest() {
        assertThatThrownBy(() -> new Streak(3, 2, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Streak(-1, 0, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
