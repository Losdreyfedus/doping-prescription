package com.doping.prescription.learner.domain;

/**
 * Decides whether a topic's mastery moved enough to announce {@code learner.mastery-changed} (docs/algoritma.md §9,
 * {@code learner.mastery.change-event-threshold}): a change of at least the threshold, in either direction, counts.
 *
 * @param threshold smallest change that counts, 0-1
 */
public record MasteryChangePolicy(double threshold) {

    /**
     * Double arithmetic cannot hold decimal steps exactly (0.5 - 0.45 = 0.04999999999999999); a change this close to
     * the threshold counts as reaching it, like the 9-decimal noise cleaning of {@link MasteryRounding}.
     */
    private static final double NOISE = 1e-9;

    public MasteryChangePolicy {
        if (threshold < 0 || threshold > 1) {
            throw new IllegalArgumentException("threshold must be within 0..1");
        }
    }

    /**
     * @param before mastery the learner saw before the signal
     * @param after  mastery after the signal
     */
    public boolean isSignificant(double before, double after) {
        return Math.abs(after - before) >= threshold - NOISE;
    }
}
