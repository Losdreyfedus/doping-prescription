package com.doping.prescription.learner.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.doping.prescription.learner.domain.MasteryBand.BandThresholds;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The example tables of docs/algoritma.md §1 and §3 as test cases. */
class MasteryCalculatorTest {

    private static final double EXACT = 1e-9;
    private static final MasteryParameters DEFAULTS = new MasteryParameters(0.3, 0.5, 5, 0.01, 1.0, 0.8, 0.4);
    private static final BandThresholds BANDS = new BandThresholds(0.6, 0.8);

    private final MasteryCalculator calculator = new MasteryCalculator(DEFAULTS);

    @Test
    void demoOlasilikMatchesTheSpecTable() {
        MasteryState start = calculator.initialState();
        MasteryState afterFirst = calculator.apply(start, SignalType.EXAM_WRONG);
        MasteryState afterSecond = calculator.apply(afterFirst, SignalType.EXAM_WRONG);
        MasteryState afterThird = calculator.apply(afterSecond, SignalType.EXAM_WRONG);
        MasteryState afterQuiz = calculator.apply(afterThird, SignalType.QUIZ_CORRECT);

        assertThat(start.mastery()).isCloseTo(0.5, within(EXACT));
        assertThat(afterFirst.mastery()).isCloseTo(0.35, within(EXACT));
        assertThat(afterSecond.mastery()).isCloseTo(0.245, within(EXACT));
        assertThat(afterThird.mastery()).isCloseTo(0.1715, within(EXACT));
        assertThat(afterQuiz.mastery()).isCloseTo(0.27092, within(EXACT));
        assertThat(api(afterThird.mastery())).isEqualTo("0.172");
        assertThat(api(afterQuiz.mastery())).isEqualTo("0.271");
        assertThat(api(afterThird.confidence())).isEqualTo("0.451");
        assertThat(api(afterQuiz.confidence())).isEqualTo("0.551");
        assertThat(afterQuiz.attempts()).isEqualTo(4);
        assertThat(afterQuiz.correct()).isEqualTo(1);
    }

    @Test
    void demoPermutasyonWithOneCorrectAndOneBlankMatchesSection3() {
        MasteryState state = calculator.apply(calculator.initialState(), SignalType.EXAM_CORRECT);
        state = calculator.apply(state, SignalType.EXAM_BLANK);

        assertThat(state.mastery()).isCloseTo(0.494, within(EXACT));
        assertThat(api(state.confidence())).isEqualTo("0.330");
    }

    @Test
    void videoSignalsDoNotChangeMastery() {
        MasteryState state = calculator.initialState();

        assertThat(calculator.apply(state, SignalType.VIDEO_COMPLETED)).isEqualTo(state);
        assertThat(calculator.apply(state, SignalType.VIDEO_SKIPPED)).isEqualTo(state);
    }

    @Test
    void decayLowersMasteryWithTimeWithoutStoringIt() {
        Instant lastSignal = Instant.parse("2026-10-01T10:00:00Z");

        assertThat(calculator.effectiveMastery(0.8, lastSignal, lastSignal)).isCloseTo(0.8, within(EXACT));
        assertThat(calculator.effectiveMastery(0.8, lastSignal, lastSignal.plus(Duration.ofDays(10))))
                .isCloseTo(0.8 * Math.exp(-0.1), within(EXACT));
        assertThat(calculator.effectiveMastery(0.8, lastSignal, lastSignal.minus(Duration.ofDays(1))))
                .isCloseTo(0.8, within(EXACT));
    }

    @Test
    void decayCountsWholeDaysSoTheExamValueHoldsOnTheSameDay() {
        Instant examAt = Instant.parse("2026-10-01T10:00:00Z");

        assertThat(calculator.effectiveMastery(0.1715, examAt, examAt.plus(Duration.ofHours(23))))
                .isEqualTo(0.1715);
        assertThat(calculator.effectiveMastery(0.1715, examAt, examAt.plus(Duration.ofHours(25))))
                .isCloseTo(0.1715 * Math.exp(-0.01), within(EXACT));
    }

    @ParameterizedTest(name = "mastery {0} with {1} signal(s) -> {2}")
    @CsvSource({
        "0.5, 0, UNKNOWN",
        "0.1715, 3, WEAK",
        "0.5999, 1, WEAK",
        "0.6, 1, MEDIUM",
        "0.7999, 2, MEDIUM",
        "0.8, 2, STRONG"
    })
    void bandsUseTheSharedThresholds(double mastery, int attempts, MasteryBand expected) {
        assertThat(MasteryBand.of(mastery, attempts, BANDS)).isEqualTo(expected);
    }

    @Test
    void trendFollowsTheLastSignal() {
        assertThat(Trend.fromLastSignal(Optional.of(SignalType.QUIZ_CORRECT))).isEqualTo(Trend.UP);
        assertThat(Trend.fromLastSignal(Optional.of(SignalType.EXAM_BLANK))).isEqualTo(Trend.DOWN);
        assertThat(Trend.fromLastSignal(Optional.of(SignalType.VIDEO_COMPLETED)))
                .isEqualTo(Trend.FLAT);
        assertThat(Trend.fromLastSignal(Optional.empty())).isEqualTo(Trend.FLAT);
    }

    @Test
    void rejectsInvalidParametersAndStates() {
        assertThatThrownBy(() -> new MasteryParameters(1.5, 0.5, 5, 0.01, 1, 0.8, 0.4))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MasteryParameters(0.3, 0.5, 0, 0.01, 1, 0.8, 0.4))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MasteryState(1.2, 0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MasteryState(0.5, 0.5, 1, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BandThresholds(0.9, 0.8)).isInstanceOf(IllegalArgumentException.class);
    }

    /** API rounding as served to clients (docs/algoritma.md §1). */
    private static String api(double value) {
        return MasteryRounding.forApi(value).toPlainString();
    }
}
