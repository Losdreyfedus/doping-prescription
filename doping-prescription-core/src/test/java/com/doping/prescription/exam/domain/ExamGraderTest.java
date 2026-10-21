package com.doping.prescription.exam.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.doping.prescription.exam.domain.ExamGrader.GradedExam;
import com.doping.prescription.exam.domain.ExamGrader.QuestionKey;
import com.doping.prescription.exam.domain.ExamGrader.TopicScore;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExamGraderTest {

    /** Mini Deneme 1: topic and correct key per question (docs/seed-verisi.md). */
    private static final List<QuestionKey> MINI_EXAM_1 = List.of(
            new QuestionKey(101, 1, "B"),
            new QuestionKey(102, 2, "D"),
            new QuestionKey(103, 3, "A"),
            new QuestionKey(104, 9, "C"),
            new QuestionKey(105, 9, "E"),
            new QuestionKey(106, 9, "B"),
            new QuestionKey(107, 8, "D"),
            new QuestionKey(108, 8, "A"),
            new QuestionKey(109, 4, "C"),
            new QuestionKey(110, 5, "E"),
            new QuestionKey(111, 6, "B"),
            new QuestionKey(112, 7, "D"));

    /** The demo scenario's answers; question 108 is left blank. */
    private static final String SCENARIO_ANSWERS = "B,D,A,A,B,D,D,-,C,E,B,D";

    @Test
    void demoScenarioGivesEightThreeOneAndNet725() {
        GradedExam graded = ExamGrader.grade(MINI_EXAM_1, answers(SCENARIO_ANSWERS));

        assertThat(graded.score()).isEqualTo(new AttemptScore(8, 3, 1, new BigDecimal("7.25")));
    }

    @Test
    void topicAnalysisShowsOlasilikAllWrongAndPermutasyonOneCorrectOneBlank() {
        GradedExam graded = ExamGrader.grade(MINI_EXAM_1, answers(SCENARIO_ANSWERS));

        assertThat(graded.topics())
                .contains(new TopicScore(9, 3, 0, 3, 0), new TopicScore(8, 2, 1, 0, 1), new TopicScore(1, 1, 1, 0, 0));
        assertThat(graded.topics()).extracting(TopicScore::topicId).containsExactly(1, 2, 3, 9, 8, 4, 5, 6, 7);
    }

    @Test
    void unansweredQuestionsAreBlank() {
        GradedExam graded = ExamGrader.grade(MINI_EXAM_1, Map.of());

        assertThat(graded.score()).isEqualTo(new AttemptScore(0, 0, 12, new BigDecimal("0.00")));
    }

    @ParameterizedTest(name = "{0} correct, {1} wrong -> net {2}")
    @CsvSource({"12, 0, 12.00", "0, 12, -3.00", "9, 3, 8.25", "10, 1, 9.75", "1, 2, 0.50"})
    void netIsCorrectMinusQuarterOfWrong(int correct, int wrong, String expectedNet) {
        Map<Integer, Optional<String>> answers = new HashMap<>();
        for (int index = 0; index < correct + wrong; index++) {
            QuestionKey key = MINI_EXAM_1.get(index);
            answers.put(key.questionId(), Optional.of(index < correct ? key.correctKey() : "Z"));
        }

        assertThat(ExamGrader.grade(MINI_EXAM_1, answers).score().net()).isEqualByComparingTo(expectedNet);
    }

    private static Map<Integer, Optional<String>> answers(String letters) {
        String[] keys = letters.split(",");
        Map<Integer, Optional<String>> answers = new HashMap<>();
        for (int index = 0; index < keys.length; index++) {
            String key = keys[index];
            answers.put(MINI_EXAM_1.get(index).questionId(), "-".equals(key) ? Optional.empty() : Optional.of(key));
        }
        return answers;
    }
}
