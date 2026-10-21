package com.doping.prescription.learner.domain;

import java.util.Optional;

/** Direction of a topic's mastery shown next to its bar on Eksiklerim. */
public enum Trend {
    UP,
    DOWN,
    FLAT;

    /**
     * The direction of the most recent mastery-affecting signal: a correct answer raises mastery, a wrong or blank one
     * lowers it.
     */
    // TODO(spec): docs/api.md says "direction of the last 3 signals", but in the demo (3 wrong, then 1 correct) any
    // majority or net-change reading of the last 3 is DOWN, while demo step 5 expects an up arrow after the
    // micro-question. The last signal's direction satisfies the demo; confirm the intended definition.
    public static Trend fromLastSignal(Optional<SignalType> lastSignal) {
        return lastSignal
                .filter(MasteryCalculator::affectsMastery)
                .map(signal -> MasteryCalculator.isCorrect(signal) ? UP : DOWN)
                .orElse(FLAT);
    }
}
