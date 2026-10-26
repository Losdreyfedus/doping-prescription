package com.doping.prescription.exam.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Grades a submitted exam: every question is CORRECT, WRONG or BLANK; net = correct - wrong / 4 (TYT rule,
 * docs/veri-modeli.md exam_attempt.net). Pure and deterministic.
 */
public final class ExamGrader {

    private static final BigDecimal WRONG_ANSWERS_PER_LOST_POINT = BigDecimal.valueOf(4);
    private static final int NET_SCALE = 2;

    private ExamGrader() {}

    /**
     * @param keys    the exam's questions with their topics and correct keys, in exam order
     * @param answers selected option per question id; missing or empty means blank
     */
    public static GradedExam grade(List<QuestionKey> keys, Map<Integer, Optional<String>> answers) {
        List<GradedQuestion> graded = new ArrayList<>(keys.size());
        for (QuestionKey key : keys) {
            Optional<String> selected = answers.getOrDefault(key.questionId(), Optional.empty());
            graded.add(new GradedQuestion(key.questionId(), key.topicId(), result(key.correctKey(), selected)));
        }
        return new GradedExam(score(graded), graded, byTopic(graded));
    }

    private static AnswerResult result(String correctKey, Optional<String> selected) {
        if (selected.isEmpty()) {
            return AnswerResult.BLANK;
        }
        return selected.get().equals(correctKey) ? AnswerResult.CORRECT : AnswerResult.WRONG;
    }

    private static AttemptScore score(List<GradedQuestion> graded) {
        int correct = count(graded, AnswerResult.CORRECT);
        int wrong = count(graded, AnswerResult.WRONG);
        int blank = count(graded, AnswerResult.BLANK);
        BigDecimal net = BigDecimal.valueOf(correct)
                .subtract(
                        BigDecimal.valueOf(wrong).divide(WRONG_ANSWERS_PER_LOST_POINT, NET_SCALE, RoundingMode.HALF_UP))
                .setScale(NET_SCALE, RoundingMode.HALF_UP);
        return new AttemptScore(correct, wrong, blank, net);
    }

    /** Topic totals in order of the topic's first question in the exam; also used for grades stored on submit. */
    public static List<TopicScore> byTopic(List<GradedQuestion> graded) {
        Map<Integer, List<GradedQuestion>> perTopic = new LinkedHashMap<>();
        graded.forEach(question -> perTopic.computeIfAbsent(question.topicId(), topic -> new ArrayList<>())
                .add(question));
        return perTopic.entrySet().stream()
                .map(entry -> new TopicScore(
                        entry.getKey(),
                        entry.getValue().size(),
                        count(entry.getValue(), AnswerResult.CORRECT),
                        count(entry.getValue(), AnswerResult.WRONG),
                        count(entry.getValue(), AnswerResult.BLANK)))
                .toList();
    }

    private static int count(List<GradedQuestion> graded, AnswerResult result) {
        return (int)
                graded.stream().filter(question -> question.result() == result).count();
    }

    /**
     * A question and its correct option.
     *
     * @param correctKey letter of the correct option
     */
    public record QuestionKey(Integer questionId, Integer topicId, String correctKey) {

        public QuestionKey {
            Objects.requireNonNull(questionId, "questionId");
            Objects.requireNonNull(topicId, "topicId");
            Objects.requireNonNull(correctKey, "correctKey");
        }
    }

    /** One graded question. */
    public record GradedQuestion(Integer questionId, Integer topicId, AnswerResult result) {}

    /** Per-topic totals for the result screen's topic analysis. */
    public record TopicScore(Integer topicId, int total, int correct, int wrong, int blank) {}

    /** The whole grading result. */
    public record GradedExam(AttemptScore score, List<GradedQuestion> questions, List<TopicScore> topics) {

        public GradedExam {
            questions = List.copyOf(questions);
            topics = List.copyOf(topics);
        }
    }
}
