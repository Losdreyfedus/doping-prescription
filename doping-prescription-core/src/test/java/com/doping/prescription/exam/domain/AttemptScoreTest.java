package com.doping.prescription.exam.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AttemptScoreTest {

    @ParameterizedTest
    @CsvSource({"-1, 0, 0", "0, -1, 0", "0, 0, -1"})
    void rejectsNegativeCounts(int correct, int wrong, int blank) {
        assertThatThrownBy(() -> new AttemptScore(correct, wrong, blank, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
