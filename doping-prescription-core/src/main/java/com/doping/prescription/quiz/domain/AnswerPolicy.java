package com.doping.prescription.quiz.domain;

/**
 * Which micro-question answers count (docs/algoritma.md §1 "Hızlı cevap", §7): an answer faster than
 * {@code quiz.min-response-ms} was given without reading the question, so it earns neither points nor a signal. Pure.
 *
 * @param minResponseMs the fastest answer that still counts
 */
public record AnswerPolicy(int minResponseMs) {

    public AnswerPolicy {
        if (minResponseMs < 0) {
            throw new IllegalArgumentException("minResponseMs must not be negative");
        }
    }

    public boolean counts(int responseMs) {
        return responseMs >= minResponseMs;
    }
}
