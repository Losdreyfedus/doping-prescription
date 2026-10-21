package com.doping.prescription.learner.domain;

/**
 * The single band definition shared by the Eksiklerim colours and the feed's weak-topic pool (docs/algoritma.md §1):
 * a red topic is exactly a topic the feed prescribes.
 */
public enum MasteryBand {
    UNKNOWN,
    WEAK,
    MEDIUM,
    STRONG;

    /**
     * @param effectiveMastery mastery after decay
     * @param attempts         mastery-affecting signals; 0 means no data
     */
    public static MasteryBand of(double effectiveMastery, int attempts, BandThresholds thresholds) {
        if (attempts == 0) {
            return UNKNOWN;
        }
        if (effectiveMastery < thresholds.weak()) {
            return WEAK;
        }
        return effectiveMastery < thresholds.strong() ? MEDIUM : STRONG;
    }

    /**
     * @param weak   below this a topic is WEAK (0.6)
     * @param strong at or above this a topic is STRONG (0.8)
     */
    public record BandThresholds(double weak, double strong) {

        public BandThresholds {
            if (weak < 0 || strong > 1 || weak > strong) {
                throw new IllegalArgumentException("thresholds must satisfy 0 <= weak <= strong <= 1");
            }
        }
    }
}
