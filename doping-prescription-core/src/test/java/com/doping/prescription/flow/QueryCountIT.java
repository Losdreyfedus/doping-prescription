package com.doping.prescription.flow;

import static org.assertj.core.api.Assertions.assertThat;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.StatementCounter;
import com.doping.prescription.auth.seed.AuthDemoDataLoader;
import com.doping.prescription.content.seed.ContentDemoDataLoader;
import com.doping.prescription.exam.api.ExamResultPublished;
import com.doping.prescription.exam.api.ExamResultPublished.AnswerOutcome;
import com.doping.prescription.learner.application.MasteryService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.json.JsonMapper;

/**
 * The feed and profile endpoints run a fixed number of SQL statements, however many cards or topics they return
 * (docs/kod-standartlari.md §2: no N+1). Runs without Redis, so the feed is composed from the database on every
 * request, its most expensive path.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Import(StatementCounter.class)
class QueryCountIT {

    private static final String SLUG = "mert";
    private static final int PERKOM = 8;
    private static final int OLASILIK = 9;
    /** Measured; a change that adds statements to these endpoints should fail here and be looked at. */
    // 13 + one query for the cards' saved marks (saved_content), however many cards the page has
    private static final int FEED_PAGE_MAX_STATEMENTS = 14;

    /** Measured; the profile lists all 10 topics, so one query per topic would exceed it. */
    private static final int PROFILE_MAX_STATEMENTS = 7;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JsonMapper json;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private StatementCounter counter;

    @Autowired
    private MasteryService mastery;

    @Autowired
    private AuthDemoDataLoader users;

    @Autowired
    private ContentDemoDataLoader content;

    private String token;

    @BeforeEach
    void learnerAfterAnExam() {
        users.load();
        content.load();
        UUID userId = jdbc.sql("select id from users where slug = :slug")
                .param("slug", SLUG)
                .query(UUID.class)
                .single();
        mastery.applyExamResult(new ExamResultPublished(
                UUID.randomUUID(),
                ExamResultPublished.SCHEMA_VERSION,
                Instant.now(),
                userId,
                UUID.randomUUID(),
                1,
                List.of(new AnswerOutcome(106, PERKOM, "CORRECT", 30), new AnswerOutcome(107, OLASILIK, "WRONG", 30))));
        token = login();
        statements("/api/feed?limit=5&refresh=true"); // warm-up: first-use initialisation is not the endpoint's cost
    }

    @Test
    void aFeedPageRunsTheSameStatementsForFiveAndForTwentyCards() {
        int fiveCards = statements("/api/feed?limit=5&refresh=true");
        int twentyCards = statements("/api/feed?limit=20&refresh=true");

        assertThat(twentyCards).isEqualTo(fiveCards).isLessThanOrEqualTo(FEED_PAGE_MAX_STATEMENTS);
    }

    @Test
    void theProfileRunsAFixedNumberOfStatementsForAllTopics() {
        assertThat(statements("/api/learner/profile")).isLessThanOrEqualTo(PROFILE_MAX_STATEMENTS);
    }

    private int statements(String uri) {
        counter.start();
        assertThat(mvc.get().uri(uri).header("Authorization", "Bearer " + token).exchange())
                .hasStatusOk();
        return counter.count();
    }

    private String login() {
        return json.readTree(mvc.post()
                        .uri("/api/auth/demo-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"" + SLUG + "\"}")
                        .exchange()
                        .getResponse()
                        .getContentAsByteArray())
                .get("token")
                .asString();
    }
}
