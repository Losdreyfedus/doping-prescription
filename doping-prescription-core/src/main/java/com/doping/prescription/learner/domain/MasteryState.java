package com.doping.prescription.learner.domain;

/**
 * A learner's mastery of one topic at full precision (docs/algoritma.md §1).
 *
 * @param mastery    0 = does not know, 1 = masters
 * @param confidence how much evidence the mastery rests on, 0-1
 * @param attempts   mastery-affecting signals so far
 * @param correct    correct ones among them
 */
public record MasteryState(double mastery, double confidence, int attempts, int correct) {

    public MasteryState {
        if (mastery < 0 || mastery > 1 || confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException("mastery and confidence must be between 0 and 1");
        }
        if (attempts < 0 || correct < 0 || correct > attempts) {
            throw new IllegalArgumentException("invalid attempt counts");
        }
    }

    public static MasteryState initial(double initialMastery) {
        return new MasteryState(initialMastery, 0, 0, 0);
    }
}
