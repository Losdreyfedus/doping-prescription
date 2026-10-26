package com.doping.prescription.learner.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MasteryChangePolicyTest {

    private final MasteryChangePolicy policy = new MasteryChangePolicy(0.05);

    @ParameterizedTest(name = "{0} -> {1} is significant: {2}")
    @CsvSource({
        // below the threshold, both directions
        "0.5,    0.549,  false",
        "0.5,    0.451,  false",
        "0.5,    0.5499999, false",
        // exactly at the threshold counts, despite 0.5 - 0.45 = 0.04999999999999999 in doubles
        "0.5,    0.55,   true",
        "0.5,    0.45,   true",
        "0.45,   0.5,    true",
        // above, both directions; the demo's 0.1715 -> 0.27092 micro-question answer
        "0.1715, 0.27092, true",
        "0.35,   0.245,  true",
        // no change
        "0.3,    0.3,    false"
    })
    void aChangeCountsFromTheThresholdOn(double before, double after, boolean significant) {
        assertThat(policy.isSignificant(before, after)).isEqualTo(significant);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, 1.01})
    void rejectsAThresholdOutsideZeroToOne(double threshold) {
        assertThatThrownBy(() -> new MasteryChangePolicy(threshold)).isInstanceOf(IllegalArgumentException.class);
    }
}
