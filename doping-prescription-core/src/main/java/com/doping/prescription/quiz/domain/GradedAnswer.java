package com.doping.prescription.quiz.domain;

import java.util.Objects;

/**
 * A micro-question answer after the server checked it.
 *
 * @param selectedKey option letter the learner chose (after undoing the option shuffle)
 * @param correct     whether it was the correct option
 * @param responseMs  time from showing the question to answering
 */
public record GradedAnswer(String selectedKey, boolean correct, int responseMs) {

    public GradedAnswer {
        Objects.requireNonNull(selectedKey, "selectedKey");
        if (responseMs < 0) {
            throw new IllegalArgumentException("responseMs must not be negative");
        }
    }
}
