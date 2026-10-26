package com.doping.prescription.quiz.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AnswerPolicyTest {

    private static final AnswerPolicy POLICY = new AnswerPolicy(2000);

    @ParameterizedTest(name = "{0} ms counts: {1}")
    @CsvSource({"0, false", "1999, false", "2000, true", "9000, true"})
    void answersFasterThanTheMinimumDoNotCount(int responseMs, boolean counts) {
        assertThat(POLICY.counts(responseMs)).isEqualTo(counts);
    }

    @Test
    void rejectsANegativeMinimum() {
        assertThatThrownBy(() -> new AnswerPolicy(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
