package com.doping.prescription.exam.domain;

import java.util.List;
import java.util.Objects;

/**
 * The text of an exam question.
 *
 * @param stem        question text
 * @param options     answer choices A-E
 * @param correctKey  letter of the correct option; revealed only in a submitted attempt's result
 * @param explanation solution shown on the result screen
 * @param difficulty  1 (easy) to 5 (hard)
 */
public record ExamQuestionContent(
        String stem, List<ExamOption> options, String correctKey, String explanation, int difficulty) {

    public ExamQuestionContent {
        Objects.requireNonNull(stem, "stem");
        options = List.copyOf(options);
        Objects.requireNonNull(correctKey, "correctKey");
        Objects.requireNonNull(explanation, "explanation");
    }
}
