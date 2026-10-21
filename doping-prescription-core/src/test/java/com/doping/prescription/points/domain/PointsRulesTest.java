package com.doping.prescription.points.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.doping.prescription.points.domain.PointsRules.Award;
import org.junit.jupiter.api.Test;

class PointsRulesTest {

    private final PointsRules rules = new PointsRules(10, 15, 5, 20, 200);

    @Test
    void aCorrectAnswerOnAWeakTopicEarnsFifteen() {
        assertThat(rules.award(true, true, 1, 0)).isEqualTo(new Award(15, 0));
        assertThat(rules.award(true, false, 1, 0)).isEqualTo(new Award(10, 0));
    }

    @Test
    void aWrongAnswerEarnsNothing() {
        assertThat(rules.award(false, true, 0, 0).total()).isZero();
    }

    @Test
    void everyFifthCorrectAnswerInARowEarnsTheBonus() {
        assertThat(rules.award(true, false, 5, 0)).isEqualTo(new Award(10, 20));
        assertThat(rules.award(true, false, 6, 0)).isEqualTo(new Award(10, 0));
        assertThat(rules.award(true, false, 10, 0)).isEqualTo(new Award(10, 20));
    }

    @Test
    void theDailyCapLimitsTheAnswerAndTheBonus() {
        assertThat(rules.award(true, true, 5, 190)).isEqualTo(new Award(10, 0));
        assertThat(rules.award(true, false, 5, 185)).isEqualTo(new Award(10, 5));
        assertThat(rules.award(true, true, 1, 200).total()).isZero();
    }
}
