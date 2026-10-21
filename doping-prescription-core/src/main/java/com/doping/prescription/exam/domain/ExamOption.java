package com.doping.prescription.exam.domain;

import java.util.Objects;

/**
 * One answer choice of an exam question (A-E).
 *
 * @param key  option letter
 * @param text option text shown to the student
 */
public record ExamOption(String key, String text) {

    public ExamOption {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(text, "text");
    }
}
