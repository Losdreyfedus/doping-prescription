package com.doping.prescription.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.doping.prescription.InfrastructureContainersConfiguration;
import com.doping.prescription.IntegrationTest;
import com.doping.prescription.auth.seed.AuthDemoDataLoader;
import com.doping.prescription.content.seed.ContentDemoDataLoader;
import com.doping.prescription.exam.seed.ExamDemoDataLoader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Phase 5 acceptance (AGENTS.md §7) on real PostgreSQL, Redis and Kafka: demo steps 1 and 3. Before the exam Deniz
 * gets a general feed; after it, the feed prescribes Olasılık and Permütasyon-Kombinasyon.
 */
@SpringBootTest(properties = IntegrationTest.JWT_SECRET_PROPERTY)
@AutoConfigureMockMvc
@Import(InfrastructureContainersConfiguration.class)
class FeedFlowIT {

    private static final String SLUG = "deniz";
    private static final int PERKOM = 8;
    private static final int OLASILIK = 9;
    private static final Duration READY_DEADLINE = Duration.ofSeconds(2);
    private static final Duration LISTENER_STARTUP = Duration.ofSeconds(60);

    /** Scenario answers of Mini Deneme 1 per question id; 108 stays blank (docs/seed-verisi.md). */
    private static final Map<Integer, String> SCENARIO = Map.ofEntries(
            Map.entry(101, "B"),
            Map.entry(102, "D"),
            Map.entry(103, "A"),
            Map.entry(104, "A"),
            Map.entry(105, "B"),
            Map.entry(106, "D"),
            Map.entry(107, "D"),
            Map.entry(109, "C"),
            Map.entry(110, "E"),
            Map.entry(111, "B"),
            Map.entry(112, "D"));

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JsonMapper json;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private KafkaListenerEndpointRegistry listeners;

    @Autowired
    private AuthDemoDataLoader users;

    @Autowired
    private ContentDemoDataLoader content;

    @Autowired
    private ExamDemoDataLoader exams;

    @BeforeEach
    void seedAndStartFresh() {
        users.load();
        content.load();
        exams.load();
        StudentReset.reset(jdbc, redis, SLUG);
        await().atMost(LISTENER_STARTUP)
                .until(() -> listeners.getListenerContainers().stream()
                        .allMatch(
                                container -> !container.getAssignedPartitions().isEmpty()));
    }

    @Test
    void theFeedTurnsIntoAPrescriptionAfterTheExam() {
        String token = login();

        JsonNode before = read(feed(token, "refresh=true"));
        assertThat(before.get("personalized").asBoolean()).isFalse();
        assertThat(before.get("cards")).hasSize(10);
        assertThat(reasons(before))
                .allSatisfy(reason ->
                        assertThat(reason).matches("TYT'de sık çıkan konu: .+|Bu hafta en çok izlenenlerden"));

        String attemptId = submitScenarioExam(token);
        await().atMost(READY_DEADLINE)
                .pollInterval(Duration.ofMillis(100))
                .until(() -> read(profile(token))
                        .path("lastExamAttemptId")
                        .asString()
                        .equals(attemptId));

        JsonNode after = read(feed(token, "refresh=true"));
        List<JsonNode> cards = cards(after);
        assertThat(after.get("personalized").asBoolean()).isTrue();
        assertThat(after.get("demoVideoSpeed").asInt()).isEqualTo(4);
        assertThat(cards.getFirst().at("/topic/topicId").asInt()).isEqualTo(OLASILIK);
        assertThat(cards.getFirst().get("reasonText").asString()).isEqualTo("Son denemende Olasılık: 3 soruda 0 doğru");
        assertThat(cards.getFirst().get("pointsAvailable").asInt()).isEqualTo(15);
        assertThat(cards.getFirst().get("hasMicroQuiz").asBoolean()).isTrue();
        List<Integer> topics =
                cards.stream().map(card -> card.at("/topic/topicId").asInt()).toList();
        assertThat(topics.subList(0, 5).stream().filter(topic -> topic == OLASILIK || topic == PERKOM))
                .hasSizeGreaterThanOrEqualTo(3);
        for (int index = 2; index < topics.size(); index++) {
            assertThat(topics.subList(index - 2, index + 1).stream().distinct()).hasSizeGreaterThan(1);
        }
        assertThat(reasons(after)).contains("TYT'de sık çıkan konu: Fonksiyonlar");

        JsonNode next = read(feed(token, "cursor=" + after.get("nextCursor").asString()));
        assertThat(next.get("sessionId").asString())
                .isEqualTo(after.get("sessionId").asString());
        assertThat(contentIds(next)).doesNotContainAnyElementsOf(contentIds(after));
        assertThat(cards(next).getFirst().get("position").asInt()).isEqualTo(10);
    }

    @Test
    void aStudyLinkShowsOnlyThatTopic() {
        JsonNode page = read(feed(login(), "topicId=" + OLASILIK));

        assertThat(cards(page))
                .isNotEmpty()
                .allSatisfy(
                        card -> assertThat(card.at("/topic/topicId").asInt()).isEqualTo(OLASILIK));
    }

    private String submitScenarioExam(String token) {
        String attemptId =
                read(post(token, "/api/exams/1/attempts")).get("attemptId").asString();
        SCENARIO.forEach((questionId, key) -> assertThat(mvc.put()
                        .uri("/api/exam-attempts/" + attemptId + "/answers/" + questionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedKey\":\"" + key + "\",\"timeSpentSec\":30}")
                        .exchange())
                .hasStatus(204));
        assertThat(post(token, "/api/exam-attempts/" + attemptId + "/submit")).hasStatusOk();
        return attemptId;
    }

    private MvcTestResult feed(String token, String query) {
        MvcTestResult result = mvc.get()
                .uri("/api/feed?" + query)
                .header("Authorization", "Bearer " + token)
                .exchange();
        assertThat(result).hasStatusOk();
        return result;
    }

    private MvcTestResult profile(String token) {
        return mvc.get()
                .uri("/api/learner/profile")
                .header("Authorization", "Bearer " + token)
                .exchange();
    }

    private MvcTestResult post(String token, String uri) {
        return mvc.post().uri(uri).header("Authorization", "Bearer " + token).exchange();
    }

    private String login() {
        return read(mvc.post()
                        .uri("/api/auth/demo-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"" + SLUG + "\"}")
                        .exchange())
                .get("token")
                .asString();
    }

    private static List<JsonNode> cards(JsonNode page) {
        return StreamSupport.stream(page.get("cards").spliterator(), false).toList();
    }

    private static List<String> reasons(JsonNode page) {
        return cards(page).stream()
                .map(card -> card.get("reasonText").asString())
                .toList();
    }

    private static List<Integer> contentIds(JsonNode page) {
        return new ArrayList<>(cards(page).stream()
                .map(card -> card.at("/content/contentId").asInt())
                .toList());
    }

    private JsonNode read(MvcTestResult result) {
        return json.readTree(result.getResponse().getContentAsByteArray());
    }
}
