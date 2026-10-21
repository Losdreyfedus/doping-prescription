package com.doping.prescription.content.domain;

import java.util.Objects;

/**
 * One answer choice of a question, e.g. {@code A: 1/3}.
 *
 * @param key  option letter
 * @param text option text shown to the student
 */
public record AnswerOption(String key, String text) {

    public AnswerOption {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(text, "text");
    }
}
