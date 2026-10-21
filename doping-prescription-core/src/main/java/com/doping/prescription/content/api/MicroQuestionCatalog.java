package com.doping.prescription.content.api;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Micro-questions for the quiz module. The answer key is part of {@link QuizQuestion} because the quiz module checks
 * answers on the server; it must never be copied into a student response, a log line or an event (AGENTS.md rule 5).
 */
public interface MicroQuestionCatalog {

    /** The video's first LIVE question the learner has not answered yet. */
    Optional<QuizQuestion> nextQuestion(Integer contentId, Set<Integer> answeredQuestionIds);

    Optional<QuizQuestion> question(Integer microQuestionId);

    /**
     * @param options    choices in their stored order, keyed A-D
     * @param correctKey stored letter of the correct choice; server-side only
     */
    record QuizQuestion(
            Integer microQuestionId,
            Integer contentId,
            Integer topicId,
            String stem,
            List<Option> options,
            String correctKey,
            String explanation) {

        public QuizQuestion {
            options = List.copyOf(options);
        }

        /** Never print the answer key. */
        @Override
        public String toString() {
            return "QuizQuestion[microQuestionId=" + microQuestionId + ", contentId=" + contentId + "]";
        }
    }

    record Option(String key, String text) {}
}
