package com.doping.prescription.exam.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Totals of a submitted attempt.
 *
 * @param correct number of correct answers
 * @param wrong   number of wrong answers
 * @param blank   number of blank answers
 * @param net     correct minus a quarter of wrong
 */
public record AttemptScore(int correct, int wrong, int blank, BigDecimal net) {

    public AttemptScore {
        if (correct < 0 || wrong < 0 || blank < 0) {
            throw new IllegalArgumentException("Answer counts must not be negative");
        }
        Objects.requireNonNull(net, "net");
    }
}
