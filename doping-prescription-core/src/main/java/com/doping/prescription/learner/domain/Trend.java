package com.doping.prescription.learner.domain;

import java.util.Optional;

/** Direction of a topic's mastery shown next to its bar on Gelişimim. */
public enum Trend {
    UP,
    DOWN,
    FLAT;

    /**
     * The direction of the most recent mastery-affecting signal: a correct answer raises mastery, a wrong or blank one
     * lowers it.
     */
    public static Trend fromLastSignal(Optional<SignalType> lastSignal) {
        return lastSignal
                .filter(MasteryCalculator::affectsMastery)
                .map(signal -> MasteryCalculator.isCorrect(signal) ? UP : DOWN)
                .orElse(FLAT);
    }
}
