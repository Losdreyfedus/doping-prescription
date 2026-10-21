package com.doping.prescription.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.doping.prescription.InfrastructureContainersConfiguration;
import com.doping.prescription.IntegrationTest;
import com.doping.prescription.auth.seed.AuthDemoDataLoader;
import com.doping.prescription.content.seed.ContentDemoDataLoader;
import com.doping.prescription.exam.seed.ExamDemoDataLoader;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Phase 4 acceptance (AGENTS.md §7): after an exam is submitted, the profile behind Eksiklerim reflects it within 2
 * seconds, through the outbox, Kafka, the learner listener and the Redis cache.
 */
@SpringBootTest(properties = IntegrationTest.JWT_SECRET_PROPERTY)
@AutoConfigureMockMvc
@Import(InfrastructureContainersConfiguration.class)
class LearnerProfileFlowIT {

    private static final String SLUG = "selin";
    private static final String USER_ID = "select id from users where slug = :slug";
    private static final Duration PROFILE_DEADLINE = Duration.ofSeconds(2);
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
        // other tests in this shared context may have used the same learner
        for (String table : List.of("exam_answer", "exam_attempt", "learner_signal", "learner_topic_mastery")) {
            String userAttempts = "attempt_id in (select id from exam_attempt where user_id = (%s))";
            String condition =
                    table.equals("exam_answer") ? userAttempts.formatted(USER_ID) : "user_id = (" + USER_ID + ")";
            jdbc.sql("delete from " + table + " where " + condition)
                    .param("slug", SLUG)
                    .update();
        }
        // warm-up: the 2-second budget is for a running system, not for the consumer group's first rebalance
        await().atMost(LISTENER_STARTUP)
                .until(() -> listeners.getListenerContainers().stream()
                        .allMatch(
                                container -> !container.getAssignedPartitions().isEmpty()));
    }

    @Test
    void submittedExamReachesEksiklerimWithinTwoSeconds() {
        String token = login();
        String attemptId =
                read(post(token, "/api/exams/1/attempts")).get("attemptId").asString();
        SCENARIO.forEach((questionId, key) -> saveAnswer(token, attemptId, questionId, key));
        assertThat(post(token, "/api/exam-attempts/" + attemptId + "/submit")).hasStatusOk();

        await().atMost(PROFILE_DEADLINE)
                .pollInterval(Duration.ofMillis(100))
                .until(() -> attemptId.equals(
                        read(profile(token)).path("lastExamAttemptId").asString()));

        MvcTestResult profile = profile(token);
        assertThat(profile).bodyJson().extractingPath("$.topics[0].name").isEqualTo("Olasılık");
        assertThat(profile).bodyJson().extractingPath("$.topics[0].mastery").isEqualTo(0.172);
        assertThat(profile).bodyJson().extractingPath("$.topics[0].confidence").isEqualTo(0.451);
        assertThat(profile).bodyJson().extractingPath("$.topics[0].band").isEqualTo("WEAK");
        assertThat(profile)
                .bodyJson()
                .extractingPath("$.topics[?(@.code == 'TYT.MAT.PERKOM')].mastery")
                .asArray()
                .containsExactly(0.494);
        assertThat(profile)
                .bodyJson()
                .extractingPath("$.topics[?(@.code == 'TYT.MAT.FONK')].band")
                .asArray()
                .containsExactly("UNKNOWN");
        assertThat(profile).bodyJson().extractingPath("$.topics[9].band").isEqualTo("UNKNOWN");
    }

    @Test
    void profileRequiresAToken() {
        assertThat(mvc.get().uri("/api/learner/profile").exchange()).hasStatus(401);
    }

    private MvcTestResult profile(String token) {
        return mvc.get()
                .uri("/api/learner/profile")
                .header("Authorization", "Bearer " + token)
                .exchange();
    }

    private void saveAnswer(String token, String attemptId, int questionId, String key) {
        assertThat(mvc.put()
                        .uri("/api/exam-attempts/" + attemptId + "/answers/" + questionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedKey\":\"" + key + "\",\"timeSpentSec\":30}")
                        .exchange())
                .hasStatus(204);
    }

    private MvcTestResult post(String token, String uri) {
        return mvc.post().uri(uri).header("Authorization", "Bearer " + token).exchange();
    }

    private String login() {
        MvcTestResult result = mvc.post()
                .uri("/api/auth/demo-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slug\":\"" + SLUG + "\"}")
                .exchange();
        return read(result).get("token").asString();
    }

    private JsonNode read(MvcTestResult result) {
        return json.readTree(result.getResponse().getContentAsByteArray());
    }
}
