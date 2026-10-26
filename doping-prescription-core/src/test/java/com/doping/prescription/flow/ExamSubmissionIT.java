package com.doping.prescription.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.doping.prescription.InfrastructureContainersConfiguration;
import com.doping.prescription.IntegrationTest;
import com.doping.prescription.TestContainers;
import com.doping.prescription.auth.seed.AuthDemoDataLoader;
import com.doping.prescription.content.seed.ContentDemoDataLoader;
import com.doping.prescription.exam.api.ExamResultPublished;
import com.doping.prescription.exam.seed.ExamDemoDataLoader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Phase 3 acceptance (AGENTS.md §7): submitting an exam publishes exam.result-published exactly once, through the
 * outbox, to a real Kafka broker.
 */
@SpringBootTest(properties = IntegrationTest.JWT_SECRET_PROPERTY)
@AutoConfigureMockMvc
@Import(InfrastructureContainersConfiguration.class)
class ExamSubmissionIT {

    private static final int MINI_EXAM_1 = 1;
    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration NO_DUPLICATE_WINDOW = Duration.ofSeconds(3);

    /** Scenario answers per question id; 108 stays blank (docs/seed-verisi.md). */
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
    private AuthDemoDataLoader users;

    @Autowired
    private ContentDemoDataLoader content;

    @Autowired
    private ExamDemoDataLoader exams;

    @BeforeEach
    void seed() {
        users.load();
        content.load();
        exams.load();
    }

    @Test
    void submittingTheScenarioPublishesTheResultEventExactlyOnce() {
        String token = login("deniz");
        String attemptId = startAttempt(token);
        SCENARIO.forEach((questionId, key) ->
                assertThat(saveAnswer(token, attemptId, questionId, key)).hasStatus(204));

        MvcTestResult result = post(token, "/api/exam-attempts/" + attemptId + "/submit");

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.correct").isEqualTo(8);
        assertThat(result).bodyJson().extractingPath("$.wrong").isEqualTo(3);
        assertThat(result).bodyJson().extractingPath("$.blank").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.net").isEqualTo(7.25);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.byTopic[?(@.topicId == 9)].name")
                .asArray()
                .containsExactly("Olasılık");
        MvcTestResult secondSubmit = post(token, "/api/exam-attempts/" + attemptId + "/submit");
        assertThat(secondSubmit).hasStatus(409);
        assertThat(secondSubmit).bodyJson().extractingPath("$.code").isEqualTo("ATTEMPT_ALREADY_SUBMITTED");
        MvcTestResult lateAnswer = saveAnswer(token, attemptId, 108, "A");
        assertThat(lateAnswer).hasStatus(409);
        assertThat(lateAnswer).bodyJson().extractingPath("$.code").isEqualTo("ATTEMPT_ALREADY_SUBMITTED");

        List<ConsumerRecord<String, String>> events = eventsFor(attemptId);
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.key()).isEqualTo("00000000-0000-4000-8000-000000000001");
            assertThat(event.value()).doesNotContain("correctKey").contains("\"result\":\"WRONG\"");
        });
    }

    @Test
    void solvingViewNeverContainsTheAnswerKey() {
        String token = login("mert");

        MvcTestResult attempt = post(token, "/api/exams/" + MINI_EXAM_1 + "/attempts");

        assertThat(attempt).hasStatus(201);
        assertThat(attempt).bodyJson().extractingPath("$.questions").asArray().hasSize(12);
        assertThat(attempt).bodyText().doesNotContain("correctKey", "explanation", "topicId", "scenario");
    }

    @Test
    void anotherLearnersAttemptIsNotFound() {
        String attemptId = startAttempt(login("mert"));

        MvcTestResult result = mvc.get()
                .uri("/api/exam-attempts/" + attemptId)
                .header("Authorization", "Bearer " + login("selin"))
                .exchange();

        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("NOT_FOUND");
    }

    @Test
    void resumingReturnsTheSameAttemptWithSavedAnswers() {
        String token = login("selin");
        String attemptId = startAttempt(token);
        saveAnswer(token, attemptId, 101, "C");

        MvcTestResult resumed = post(token, "/api/exams/" + MINI_EXAM_1 + "/attempts");

        assertThat(resumed).bodyJson().extractingPath("$.attemptId").isEqualTo(attemptId);
        assertThat(resumed)
                .bodyJson()
                .extractingPath("$.answers[?(@.questionId == 101)].selectedKey")
                .asArray()
                .containsExactly("C");
    }

    private List<ConsumerRecord<String, String>> eventsFor(String attemptId) {
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties())) {
            consumer.subscribe(List.of(ExamResultPublished.TOPIC));
            List<ConsumerRecord<String, String>> matching = new ArrayList<>();
            await().atMost(EVENT_TIMEOUT).until(() -> {
                consumer.poll(Duration.ofMillis(500)).forEach(record -> {
                    if (record.value().contains(attemptId)) {
                        matching.add(record);
                    }
                });
                return !matching.isEmpty();
            });
            // keep listening a little longer: a duplicate would arrive right after the first message
            await().pollDelay(NO_DUPLICATE_WINDOW)
                    .atMost(NO_DUPLICATE_WINDOW.plusSeconds(2))
                    .until(() -> {
                        consumer.poll(Duration.ofMillis(500)).forEach(record -> {
                            if (record.value().contains(attemptId)) {
                                matching.add(record);
                            }
                        });
                        return true;
                    });
            return matching;
        }
    }

    private static Properties consumerProperties() {
        Properties properties = new Properties();
        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, TestContainers.kafka().getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "exam-it-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return properties;
    }

    private String startAttempt(String token) {
        return read(post(token, "/api/exams/" + MINI_EXAM_1 + "/attempts"))
                .get("attemptId")
                .asString();
    }

    private MvcTestResult saveAnswer(String token, String attemptId, int questionId, String key) {
        return mvc.put()
                .uri("/api/exam-attempts/" + attemptId + "/answers/" + questionId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"selectedKey\":\"" + key + "\",\"timeSpentSec\":30}")
                .exchange();
    }

    private MvcTestResult post(String token, String uri) {
        return mvc.post().uri(uri).header("Authorization", "Bearer " + token).exchange();
    }

    private String login(String slug) {
        MvcTestResult result = mvc.post()
                .uri("/api/auth/demo-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slug\":\"" + slug + "\"}")
                .exchange();
        return read(result).get("token").asString();
    }

    private JsonNode read(MvcTestResult result) {
        return json.readTree(result.getResponse().getContentAsByteArray());
    }
}
