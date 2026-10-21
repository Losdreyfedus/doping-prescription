package com.doping.prescription.learner.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * The mastery model of docs/algoritma.md §1: every signal pulls mastery a step towards its outcome, confidence grows
 * with the number of signals, and forgetting (decay) is applied when mastery is read. Pure and deterministic.
 */
public final class MasteryCalculator {

    private final MasteryParameters parameters;

    public MasteryCalculator(MasteryParameters parameters) {
        this.parameters = Objects.requireNonNull(parameters, "parameters");
    }

    public MasteryState initialState() {
        return MasteryState.initial(parameters.initial());
    }

    /** Whether the signal changes mastery; video signals only feed other rules (§1 table). */
    public static boolean affectsMastery(SignalType type) {
        return type != SignalType.VIDEO_COMPLETED && type != SignalType.VIDEO_SKIPPED;
    }

    /** {@code m' = m + alpha * w * (y - m)}; {@code confidence = 1 - exp(-attempts / k)} (§1). */
    public MasteryState apply(MasteryState state, SignalType signal) {
        if (!affectsMastery(signal)) {
            return state;
        }
        boolean correct = isCorrect(signal);
        double outcome = correct ? 1 : 0;
        double mastery =
                state.mastery() + parameters.alpha() * parameters.weightOf(signal) * (outcome - state.mastery());
        int attempts = state.attempts() + 1;
        double confidence = 1 - Math.exp(-attempts / parameters.confidenceK());
        return new MasteryState(clamp(mastery), confidence, attempts, state.correct() + (correct ? 1 : 0));
    }

    /**
     * {@code m_effective = m * exp(-lambda * days since the last signal)}; never stored (§1, Unutma). Days are whole
     * elapsed days, so mastery read minutes after an exam equals the stored value and matches the documented examples.
     */
    public double effectiveMastery(double mastery, Instant lastSignalAt, Instant now) {
        long days = Math.max(0, Duration.between(lastSignalAt, now).toDays());
        return mastery * Math.exp(-parameters.decayLambda() * days);
    }

    public static boolean isCorrect(SignalType signal) {
        return signal == SignalType.EXAM_CORRECT || signal == SignalType.QUIZ_CORRECT;
    }

    private static double clamp(double value) {
        return Math.min(1, Math.max(0, value));
    }
}
