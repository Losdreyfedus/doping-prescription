package com.doping.prescription.admin.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.auth.seed.AuthDemoDataLoader;
import com.doping.prescription.content.seed.ContentDemoDataLoader;
import com.doping.prescription.exam.api.ExamResultPublished;
import com.doping.prescription.exam.api.ExamResultPublished.AnswerOutcome;
import com.doping.prescription.exam.seed.ExamDemoDataLoader;
import com.doping.prescription.feed.application.FeedService;
import com.doping.prescription.learner.application.MasteryService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

/** Phase 7 acceptance (AGENTS.md §7): reset plus SCRIPTED_IN_PROGRESS prepares the demo; the debug API explains the feed. */
@IntegrationTest
@AutoConfigureMockMvc
class DemoControlIT {

    private static final String SLUG = "deniz";
    private static final int OLASILIK = 9;
    private static final Duration RESET_BUDGET = Duration.ofSeconds(1);

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JsonMapper json;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private MasteryService mastery;

    @Autowired
    private FeedService feed;

    @Autowired
    private AuthDemoDataLoader users;

    @Autowired
    private ContentDemoDataLoader content;

    @Autowired
    private ExamDemoDataLoader exams;

    private UUID deniz;
    private String admin;

    @BeforeEach
    void seed() {
        users.load();
        content.load();
        exams.load();
        deniz = jdbc.sql("select id from users where slug = :slug")
                .param("slug", SLUG)
                .query(UUID.class)
                .single();
        admin = login("admin");
    }

    @Test
    void resetErasesEveryModulesData() {
        // Given
        weakOlasilik();
        feed.page(deniz, null, 10, null, true);

        // When
        assertThat(post("/api/admin/demo/reset", "{\"slug\":\"deniz\"}", admin)).hasStatus(204);

        // Then
        for (String table : List.of(
                "exam_attempt",
                "learner_signal",
                "learner_topic_mastery",
                "feed_impression",
                "micro_quiz_attempt",
                "points_ledger")) {
            assertThat(count(table)).as(table).isZero();
        }
    }

    /**
     * docs/demo-senaryosu.md: "Sıfırla" takes under a second on stage. Measured on a warm application (weakOlasilik
     * already reset once) with the monotonic clock; the budget is the requirement itself, a typical reset takes far
     * less, so only a real slowdown fails it.
     */
    @Test
    void resetOfAWarmApplicationTakesUnderOneSecond() {
        // Given
        weakOlasilik();
        feed.page(deniz, null, 10, null, true);

        // When
        long started = System.nanoTime();
        MvcTestResult reset = post("/api/admin/demo/reset", "{\"slug\":\"deniz\"}", admin);
        Duration took = Duration.ofNanos(System.nanoTime() - started);

        // Then
        assertThat(reset).hasStatus(204);
        assertThat(took).isLessThan(RESET_BUDGET);
    }

    @Test
    void feedDebugWithoutSlugIsAnInvalidRequestProblem() {
        // When
        MvcTestResult missing = mvc.get()
                .uri("/api/admin/feed-debug")
                .header("Authorization", "Bearer " + admin)
                .exchange();
        MvcTestResult tooLong = mvc.get()
                .uri("/api/admin/feed-debug?slug=" + "x".repeat(33))
                .header("Authorization", "Bearer " + admin)
                .exchange();

        // Then
        assertThat(missing).hasStatus(400);
        assertThat(missing).bodyJson().extractingPath("$.code").isEqualTo("INVALID_REQUEST");
        assertThat(tooLong).hasStatus(400);
        assertThat(tooLong).bodyJson().extractingPath("$.code").isEqualTo("INVALID_REQUEST");
    }

    @Test
    void aWrongMethodIsAMethodNotAllowedProblem() {
        // When
        MvcTestResult result = mvc.get()
                .uri("/api/admin/demo/reset")
                .header("Authorization", "Bearer " + admin)
                .exchange();

        // Then
        assertThat(result).hasStatus(405);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("METHOD_NOT_ALLOWED");
    }

    @Test
    void scriptedInProgressLeavesTheScenarioExamMarkedButNotSubmitted() {
        weakOlasilik();

        assertThat(post("/api/admin/demo/persona", "{\"slug\":\"deniz\",\"persona\":\"SCRIPTED_IN_PROGRESS\"}", admin))
                .hasStatus(204);

        assertThat(jdbc.sql("select status from exam_attempt where user_id = :userId")
                        .param("userId", deniz)
                        .query(String.class)
                        .list())
                .containsExactly("IN_PROGRESS");
        assertThat(jdbc.sql("""
                        select count(*) from exam_answer answer join exam_attempt attempt on attempt.id = answer.attempt_id
                        where attempt.user_id = :userId and answer.selected_key is not null
                        """).param("userId", deniz).query(Integer.class).single())
                .isEqualTo(11);
        assertThat(count("learner_signal"))
                .as("no signals before the exam is submitted")
                .isZero();
    }

    @Test
    void unknownPersonasAndStudentsAreRejected() {
        MvcTestResult unknown =
                post("/api/admin/demo/persona", "{\"slug\":\"deniz\",\"persona\":\"WEAK_PROBLEMS\"}", admin);
        assertThat(unknown).hasStatus(400);
        assertThat(unknown).bodyJson().extractingPath("$.code").isEqualTo("INVALID_REQUEST");
        assertThat(post("/api/admin/demo/reset", "{\"slug\":\"admin\"}", admin)).hasStatus(404);
        assertThat(post("/api/admin/demo/reset", "{\"slug\":\"deniz\"}", login("deniz")))
                .hasStatus(403);
    }

    @Test
    void theAnswerKeyListsMiniExamOneWithTheBlankScenarioAnswer() {
        MvcTestResult key = mvc.get()
                .uri("/api/admin/demo/answer-key")
                .header("Authorization", "Bearer " + admin)
                .exchange();

        assertThat(key).hasStatusOk();
        assertThat(key).bodyJson().extractingPath("$.length()").isEqualTo(12);
        assertThat(key).bodyJson().extractingPath("$[7].scenarioKey").isNull();
        assertThat(key).bodyJson().extractingPath("$[0].correctKey").isNotNull();
    }

    @Test
    void feedDebugShowsTheWeakestTopicsAndEveryCardsScoreComponents() {
        weakOlasilik();
        feed.page(deniz, null, 10, null, true);

        MvcTestResult debug = mvc.get()
                .uri("/api/admin/feed-debug?slug=deniz")
                .header("Authorization", "Bearer " + admin)
                .exchange();

        assertThat(debug).hasStatusOk();
        assertThat(debug).bodyJson().extractingPath("$.weakestTopics[0].name").isEqualTo("Olasılık");
        assertThat(debug).bodyJson().extractingPath("$.cards.length()").isEqualTo(10);
        assertThat(debug).bodyJson().extractingPath("$.cards[0].pool").isEqualTo("WEAK_TOPIC");
        assertThat(debug).bodyJson().extractingPath("$.cards[0].weakness").isNotNull();
        assertThat(debug).bodyJson().extractingPath("$.cards[0].difficultyFit").isNotNull();
    }

    private void weakOlasilik() {
        post("/api/admin/demo/reset", "{\"slug\":\"deniz\"}", admin);
        mastery.applyExamResult(new ExamResultPublished(
                UUID.randomUUID(),
                ExamResultPublished.SCHEMA_VERSION,
                Instant.now(),
                deniz,
                UUID.randomUUID(),
                1,
                List.of(
                        new AnswerOutcome(107, OLASILIK, "WRONG", 30),
                        new AnswerOutcome(109, OLASILIK, "WRONG", 30),
                        new AnswerOutcome(110, OLASILIK, "WRONG", 30))));
    }

    private int count(String table) {
        return jdbc.sql("select count(*) from " + table + " where user_id = :userId")
                .param("userId", deniz)
                .query(Integer.class)
                .single();
    }

    private MvcTestResult post(String uri, String body, String token) {
        return mvc.post()
                .uri(uri)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private String login(String slug) {
        return json.readTree(mvc.post()
                        .uri("/api/auth/demo-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"" + slug + "\"}")
                        .exchange()
                        .getResponse()
                        .getContentAsByteArray())
                .get("token")
                .asString();
    }
}
