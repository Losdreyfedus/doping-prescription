package com.doping.prescription.points.domain;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A learner's run of consecutive correct micro-question answers (docs/algoritma.md §7): a correct answer extends it, a
 * wrong one ends it, and the same answer ({@code refId}) applied twice counts once. Pure and immutable.
 *
 * @param current   correct answers in a row so far
 * @param best      the longest run ever
 * @param lastRefId the last answer applied; null before the first
 */
public record Streak(int current, int best, @Nullable String lastRefId) {

    public static final Streak NONE = new Streak(0, 0, null);

    public Streak {
        if (current < 0 || best < current) {
            throw new IllegalArgumentException("streak must satisfy 0 <= current <= best");
        }
    }

    /** The streak after the answer, or this one if the answer was already applied. */
    public Streak apply(String refId, boolean correct) {
        Objects.requireNonNull(refId, "refId");
        if (refId.equals(lastRefId)) {
            return this;
        }
        int next = correct ? current + 1 : 0;
        return new Streak(next, Math.max(best, next), refId);
    }
}
