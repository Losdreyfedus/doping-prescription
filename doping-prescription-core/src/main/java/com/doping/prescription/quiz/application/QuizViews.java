package com.doping.prescription.quiz.application;

import java.util.List;
import java.util.UUID;

/** Response bodies of the quiz endpoints (docs/api.md). None of them carries the answer key before answering. */
public final class QuizViews {

    private QuizViews() {}

    /**
     * @param options    shuffled and relabelled A-D
     * @param quizToken  signed; send it back with the answer
     */
    public record MicroQuiz(
            Integer microQuestionId,
            UUID impressionId,
            String stem,
            List<OptionView> options,
            int timeLimitSec,
            int pointsAvailable,
            String quizToken) {

        public MicroQuiz {
            options = List.copyOf(options);
        }
    }

    public record OptionView(String key, String text) {}

    /**
     * @param correctKey    the shown letter of the correct option, revealed only after answering
     * @param pointsAwarded 0 for a second answer, a too fast answer or a wrong one
     */
    public record QuizResult(
            boolean isCorrect,
            String correctKey,
            String explanation,
            int pointsAwarded,
            int totalPoints,
            StreakView streak,
            MasteryChangeView masteryChange) {}

    /**
     * @param bonusAt streak length that earns the bonus
     */
    public record StreakView(int current, int bonusAt) {}

    /**
     * @param before mastery before the answer, 3 decimals
     * @param after  mastery after it, 3 decimals
     */
    public record MasteryChangeView(Integer topicId, String name, double before, double after) {}
}
