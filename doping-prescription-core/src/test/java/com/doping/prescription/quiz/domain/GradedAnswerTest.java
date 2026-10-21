package com.doping.prescription.quiz.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GradedAnswerTest {

    @Test
    void rejectsNegativeResponseTime() {
        assertThatThrownBy(() -> new GradedAnswer("A", true, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
