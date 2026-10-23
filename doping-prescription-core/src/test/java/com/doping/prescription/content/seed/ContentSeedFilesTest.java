package com.doping.prescription.content.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.doping.prescription.seedcheck.SeedQuestionCheck;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Checks the content seed files against the rules of docs/seed-verisi.md, without a database. */
class ContentSeedFilesTest {

    private static final int OLASILIK = 9;
    private static final int PERMUTASYON = 8;
    private static final Set<Integer> UNTAGGED_VIDEOS = Set.of(8, 20, 28, 40);
    private static final List<String> TEACHERS =
            List.of("Ayşe Öğretmen", "Burak Öğretmen", "Can Öğretmen", "Derya Öğretmen");

    /** Questions that cannot be checked by an expression; adding one here is a conscious decision. */
    private static final Set<Integer> EXPECTED_MANUAL_CHECKS = Set.of(25);

    private final List<JsonNode> videos = items("seed/videos.json");
    private final List<JsonNode> questions = items("seed/micro-questions.json");

    @Test
    void everySeedFileCarriesTheEditorialNote() {
        for (String file :
                List.of("seed/users.json", "seed/topics.json", "seed/videos.json", "seed/micro-questions.json")) {
            assertThat(SeedQuestionCheck.readSeedFile(file).get("_note").asString())
                    .as(file)
                    .startsWith("Editör kontrolünden geçmedi");
        }
    }

    @Test
    void fortyVideosFourPerTopicWithFormulaIds() {
        assertThat(videos).hasSize(40);
        for (JsonNode video : videos) {
            int id = video.get("id").asInt();
            int topicId = video.get("topicId").asInt();
            assertThat((id - 1) / 4 + 1).as("topic of video %s", id).isEqualTo(topicId);
            assertThat(video.get("teacherName").asString()).isEqualTo(TEACHERS.get((id - 1) % 4));
            assertThat(video.get("durationSec").asInt()).isBetween(20, 85);
            assertThat(video.get("qualityScore").asDouble()).isBetween(0.55, 0.95);
        }
    }

    @Test
    void everyTopicHasDifficultiesOneToFourAllKindsAndDistinctQuality() {
        Map<Integer, List<JsonNode>> byTopic = videos.stream()
                .collect(Collectors.groupingBy(video -> video.get("topicId").asInt()));

        assertThat(byTopic).hasSize(10);
        byTopic.forEach((topicId, topicVideos) -> {
            assertThat(topicVideos)
                    .extracting(video -> video.get("difficulty").asInt())
                    .as("difficulties of topic %s", topicId)
                    .containsExactlyInAnyOrder(1, 2, 3, 4);
            assertThat(topicVideos.stream()
                            .map(video -> video.get("kind").asString())
                            .collect(Collectors.toSet()))
                    .as("kinds of topic %s", topicId)
                    .containsExactlyInAnyOrder("CONCEPT", "SOLVED_EXAMPLE", "TIP");
            assertThat(topicVideos)
                    .extracting(video -> video.get("qualityScore").asDouble())
                    .as("quality of topic %s", topicId)
                    .doesNotHaveDuplicates();
        });
    }

    @Test
    void onlyTheFourthVideosOfTopicsTwoFiveSevenTenAreUntagged() {
        for (JsonNode video : videos) {
            int id = video.get("id").asInt();
            boolean draft = UNTAGGED_VIDEOS.contains(id);
            assertThat(video.get("status").asString()).as("video %s", id).isEqualTo(draft ? "DRAFT" : "LIVE");
            assertThat(video.get("publishedDaysAgo").isNull())
                    .as("video %s", id)
                    .isEqualTo(draft);
        }
    }

    @Test
    void microQuestionsCoverEveryVideoTwiceForOlasilikAndPermutasyon() {
        Map<Integer, Integer> topicOfVideo = new HashMap<>();
        videos.forEach(video ->
                topicOfVideo.put(video.get("id").asInt(), video.get("topicId").asInt()));
        Map<Integer, Integer> questionsPerVideo = new TreeMap<>();
        for (JsonNode question : questions) {
            int contentId = question.get("contentId").asInt();
            questionsPerVideo.merge(contentId, 1, Integer::sum);
            assertThat(question.get("topicId").asInt())
                    .as("topic of question %s", question.get("id"))
                    .isEqualTo(topicOfVideo.get(contentId));
        }

        assertThat(questions).hasSize(48);
        assertThat(questions)
                .extracting(question -> question.get("id").asInt())
                .containsExactlyElementsOf(IntStream.rangeClosed(1, 48).boxed().toList());
        questionsPerVideo.forEach((videoId, count) -> {
            int topicId = topicOfVideo.get(videoId);
            int expected = topicId == OLASILIK || topicId == PERMUTASYON ? 2 : 1;
            assertThat(count).as("questions of video %s", videoId).isEqualTo(expected);
        });
        assertThat(questionsPerVideo).hasSize(40);
    }

    @Test
    void microQuestionsHaveFourDistinctOptionsAndAnExplanation() {
        Set<String> stems = new HashSet<>();
        for (JsonNode question : questions) {
            String label = "micro-question " + question.get("id");
            assertThat(SeedQuestionCheck.optionTexts(question))
                    .as(label)
                    .hasSize(4)
                    .doesNotHaveDuplicates();
            assertThat(question.get("correctKey").asString()).as(label).isIn("A", "B", "C", "D");
            assertThat(question.get("explanation").asString()).as(label).isNotBlank();
            assertThat(stems.add(question.get("stem").asString()))
                    .as("%s stem is unique", label)
                    .isTrue();
        }
    }

    @Test
    void firstOlasilikQuestionIsTheApiExample() {
        JsonNode first = questions.stream()
                .filter(question -> question.get("contentId").asInt() == 33)
                .findFirst()
                .orElseThrow();

        assertThat(first.get("id").asInt()).isEqualTo(37);
        assertThat(first.get("stem").asString()).contains("çift olduğu bilindiğine göre 4 olma olasılığı");
        assertThat(SeedQuestionCheck.optionTexts(first)).containsExactly("1/6", "1/3", "1/2", "2/3");
        assertThat(first.get("correctKey").asString()).isEqualTo("B");
    }

    @Test
    void everyMicroQuestionHasExactlyOneCorrectAnswer() {
        List<Integer> manual = new ArrayList<>();
        for (JsonNode question : questions) {
            SeedQuestionCheck.verify("micro-question " + question.get("id"), question)
                    .ifPresent(reason -> manual.add(question.get("id").asInt()));
        }

        assertThat(manual).containsExactlyInAnyOrderElementsOf(EXPECTED_MANUAL_CHECKS);
    }

    private static List<JsonNode> items(String path) {
        List<JsonNode> items = new ArrayList<>();
        SeedQuestionCheck.readSeedFile(path).get("items").forEach(items::add);
        return items;
    }
}
