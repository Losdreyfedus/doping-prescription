package com.doping.prescription.exam.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.doping.prescription.seedcheck.SeedQuestionCheck;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Checks seed/exams.json against docs/seed-verisi.md, without a database. */
class ExamSeedFileTest {

    private static final int PROBLEMLER = 6;

    /** docs/seed-verisi.md, Mini Deneme 1: topic, correct key and scenario answer per question (null = blank). */
    private static final List<List<Object>> MINI_EXAM_1 = List.of(
            List.of(1, "B", "B"),
            List.of(2, "D", "D"),
            List.of(3, "A", "A"),
            List.of(9, "C", "A"),
            List.of(9, "E", "B"),
            List.of(9, "B", "D"),
            List.of(8, "D", "D"),
            List.of(8, "A", ""),
            List.of(4, "C", "C"),
            List.of(5, "E", "E"),
            List.of(6, "B", "B"),
            List.of(7, "D", "D"));

    private final JsonNode file = SeedQuestionCheck.readSeedFile("seed/exams.json");

    @Test
    void carriesTheEditorialNote() {
        assertThat(file.get("_note").asString()).startsWith("Editör kontrolünden geçmedi");
    }

    @Test
    void miniExam1MatchesTheFixedAnswerKeyOfTheScenario() {
        List<JsonNode> questions = questions(1);

        assertThat(questions).hasSize(12);
        for (int index = 0; index < questions.size(); index++) {
            JsonNode question = questions.get(index);
            List<Object> expected = MINI_EXAM_1.get(index);
            String label = "Mini Deneme 1 soru " + (index + 1);
            assertThat(question.get("orderNo").asInt()).as(label).isEqualTo(index + 1);
            assertThat(question.get("topicId").asInt()).as(label).isEqualTo(expected.get(0));
            assertThat(question.get("correctKey").asString()).as(label).isEqualTo(expected.get(1));
            assertThat(scenarioAnswer(question)).as(label).isEqualTo(expected.get(2));
        }
    }

    @Test
    void scenarioAnswersGiveEightCorrectThreeWrongOneBlankAndNet725() {
        int correct = 0;
        int wrong = 0;
        int blank = 0;
        for (JsonNode question : questions(1)) {
            String answer = scenarioAnswer(question);
            if (answer.isEmpty()) {
                blank++;
            } else if (answer.equals(question.get("correctKey").asString())) {
                correct++;
            } else {
                wrong++;
            }
        }
        BigDecimal net =
                BigDecimal.valueOf(correct).subtract(BigDecimal.valueOf(wrong).divide(BigDecimal.valueOf(4)));

        assertThat(List.of(correct, wrong, blank)).containsExactly(8, 3, 1);
        assertThat(net).isEqualByComparingTo("7.25");
    }

    @Test
    void miniExam2HasFourProblemsAndOneQuestionOfEightOtherTopicsWithBalancedKeys() {
        List<JsonNode> questions = questions(2);
        Map<Integer, Long> perTopic = questions.stream()
                .collect(Collectors.groupingBy(
                        question -> question.get("topicId").asInt(), Collectors.counting()));
        Map<String, Long> perKey = questions.stream()
                .collect(Collectors.groupingBy(
                        question -> question.get("correctKey").asString(), Collectors.counting()));

        assertThat(questions).hasSize(12);
        assertThat(perTopic).containsEntry(PROBLEMLER, 4L).hasSize(9).doesNotContainKey(8);
        perTopic.forEach((topic, count) -> assertThat(count).isEqualTo(topic == PROBLEMLER ? 4 : 1));
        assertThat(perKey.keySet()).containsExactlyInAnyOrder("A", "B", "C", "D", "E");
        assertThat(perKey.values()).allSatisfy(count -> assertThat(count).isBetween(2L, 3L));
    }

    @Test
    void fonksiyonlarIsNotInTheScenarioExam() {
        assertThat(questions(1)).noneMatch(question -> question.get("topicId").asInt() == 10);
    }

    @Test
    void everyQuestionHasFiveDistinctOptionsAndExactlyOneCorrectAnswer() {
        Set<String> stems = new HashSet<>();
        List<String> manual = new ArrayList<>();
        for (int examId : List.of(1, 2)) {
            for (JsonNode question : questions(examId)) {
                String label = "deneme " + examId + " soru " + question.get("orderNo");
                assertThat(SeedQuestionCheck.optionTexts(question))
                        .as(label)
                        .hasSize(5)
                        .doesNotHaveDuplicates();
                assertThat(question.get("correctKey").asString()).as(label).isIn("A", "B", "C", "D", "E");
                assertThat(question.get("explanation").asString()).as(label).isNotBlank();
                assertThat(stems.add(question.get("stem").asString()))
                        .as("%s stem is unique", label)
                        .isTrue();
                SeedQuestionCheck.verify(label, question).ifPresent(reason -> manual.add(label));
            }
        }

        assertThat(manual).as("exam questions without a computed check").isEmpty();
    }

    private List<JsonNode> questions(int examId) {
        Map<Integer, JsonNode> exams = new HashMap<>();
        file.get("items").forEach(exam -> exams.put(exam.get("id").asInt(), exam));
        List<JsonNode> questions = new ArrayList<>();
        exams.get(examId).get("questions").forEach(questions::add);
        return questions;
    }

    private static String scenarioAnswer(JsonNode question) {
        JsonNode answer = question.get("scenarioAnswer");
        return answer.isNull() ? "" : answer.asString();
    }
}
