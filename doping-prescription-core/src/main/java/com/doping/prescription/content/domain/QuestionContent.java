package com.doping.prescription.content.domain;

import java.util.List;
import java.util.Objects;

/**
 * The text of a micro-question.
 *
 * @param stem        question text
 * @param options     answer choices A-D
 * @param correctKey  letter of the correct option; never sent to students
 * @param explanation short solution shown after answering
 * @param difficulty  1 (easy) to 5 (hard)
 */
public record QuestionContent(
        String stem, List<AnswerOption> options, String correctKey, String explanation, int difficulty) {

    public QuestionContent {
        Objects.requireNonNull(stem, "stem");
        options = List.copyOf(options);
        Objects.requireNonNull(correctKey, "correctKey");
        Objects.requireNonNull(explanation, "explanation");
    }
}
