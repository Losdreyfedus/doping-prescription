package com.doping.prescription.quiz.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.auth.seed.AuthDemoDataLoader;
import com.doping.prescription.content.api.MicroQuestionCatalog;
import com.doping.prescription.content.seed.ContentDemoDataLoader;
import com.doping.prescription.exam.api.ExamResultPublished;
import com.doping.prescription.exam.api.ExamResultPublished.AnswerOutcome;
import com.doping.prescription.feed.application.FeedService;
import com.doping.prescription.feed.application.FeedViews.CardView;
import com.doping.prescription.feed.application.InteractionService;
import com.doping.prescription.feed.application.InteractionService.Interaction;
import com.doping.prescription.feed.domain.InteractionType;
import com.doping.prescription.learner.application.MasteryService;
import com.doping.prescription.quiz.application.QuizTokens.QuizClaims;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Phase 6 acceptance (AGENTS.md §7): every quiz error code of docs/api.md, a second answer earning 0 points and two
 * concurrent answers earning the points once. Runs without Redis, so the database constraint alone guards duplicates.
 */
@IntegrationTest
@AutoConfigureMockMvc
class QuizIT {

    private static final String SLUG = "mert";
    private static final int PERKOM = 8;
    private static final int OLASILIK = 9;
    private static final int SLOW_ENOUGH_MS = 9000;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JsonMapper json;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private FeedService feed;

    @Autowired
    private InteractionService interactions;

    @Autowired
    private MasteryService mastery;

    @Autowired
    private QuizService quizzes;

    @Autowired
    private QuizTokens tokens;

    @Autowired
    private QuizProperties properties;

    @Autowired
    private MicroQuestionCatalog questions;

    @Autowired
    private AuthDemoDataLoader users;

    @Autowired
    private ContentDemoDataLoader content;

    private UUID userId;
    private String token;

    @BeforeEach
    void freshLearner() {
        users.load();
        content.load();
        userId = jdbc.sql("select id from users where slug = :slug")
                .param("slug", SLUG)
                .query(UUID.class)
                .single();
        List.of(
                        "delete from micro_quiz_attempt where user_id = :userId",
                        "delete from points_ledger where user_id = :userId",
                        "delete from points_streak where user_id = :userId",
                        "delete from feed_interaction where user_id = :userId",
                        "delete from feed_impression where user_id = :userId",
                        "delete from learner_signal where user_id = :userId",
                        "delete from learner_topic_mastery where user_id = :userId")
                .forEach(sql -> jdbc.sql(sql).param("userId", userId).update());
        token = login(SLUG);
    }

    @Test
    void aCorrectAnswerOnAWeakTopicEarnsFifteenPointsAndRaisesMastery() {
        weakOlasilik();
        CardView card = firstCard(OLASILIK);
        watch(card);

        JsonNode quiz = openQuiz(card).getFirst();
        MvcTestResult result = answer(quiz, correctShownKey(quiz), SLOW_ENOUGH_MS);

        assertThat(result).hasStatusOk();
        JsonNode body = read(result);
        assertThat(body.get("isCorrect").asBoolean()).isTrue();
        assertThat(body.get("pointsAwarded").asInt()).isEqualTo(15);
        assertThat(body.get("totalPoints").asInt()).isEqualTo(15);
        assertThat(body.at("/streak/current").asInt()).isEqualTo(1);
        assertThat(body.at("/masteryChange/name").asString()).isEqualTo("Olasılık");
        assertThat(body.at("/masteryChange/before").asDouble()).isEqualTo(0.172);
        assertThat(body.at("/masteryChange/after").asDouble()).isEqualTo(0.271);
        assertThat(result).bodyText().doesNotContain("correct_key");
        assertThat(read(get("/api/points")).get("total").asInt()).isEqualTo(15);
    }

    @Test
    void aSecondAnswerToTheSameQuestionEarnsNothing() {
        CardView card = firstCard(null);
        watch(card);
        JsonNode first = openQuiz(card).getFirst();
        answer(first, correctShownKey(first), SLOW_ENOUGH_MS);

        JsonNode reissued = reissue(first);
        MvcTestResult second = answer(reissued, correctShownKey(reissued), SLOW_ENOUGH_MS);

        assertThat(second).hasStatusOk();
        assertThat(read(second).get("pointsAwarded").asInt()).isZero();
        assertThat(read(get("/api/points")).get("total").asInt()).isEqualTo(10);
    }

    @Test
    void twoConcurrentAnswersEarnThePointsOnce() throws Exception {
        CardView card = firstCard(null);
        watch(card);
        JsonNode quiz = openQuiz(card).getFirst();
        JsonNode twin = reissue(quiz);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> answerFirst = () -> {
            start.await();
            return quizzes.answer(
                            userId,
                            quiz.get("microQuestionId").asInt(),
                            quiz.get("quizToken").asString(),
                            correctShownKey(quiz),
                            SLOW_ENOUGH_MS)
                    .pointsAwarded();
        };
        Callable<Integer> answerTwin = () -> {
            start.await();
            return quizzes.answer(
                            userId,
                            twin.get("microQuestionId").asInt(),
                            twin.get("quizToken").asString(),
                            correctShownKey(twin),
                            SLOW_ENOUGH_MS)
                    .pointsAwarded();
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> one = pool.submit(answerFirst);
            Future<Integer> other = pool.submit(answerTwin);
            start.countDown();
            assertThat(List.of(one.get(), other.get())).containsExactlyInAnyOrder(10, 0);
        } finally {
            pool.shutdown();
        }
        assertThat(jdbc.sql("select count(*) from micro_quiz_attempt where user_id = :userId")
                        .param("userId", userId)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
    }

    @Test
    void aTooFastAnswerEarnsNothing() {
        CardView card = firstCard(null);
        watch(card);
        JsonNode quiz = openQuiz(card).getFirst();

        JsonNode body = read(answer(quiz, correctShownKey(quiz), properties.minResponseMs() - 1));

        assertThat(body.get("isCorrect").asBoolean()).isTrue();
        assertThat(body.get("pointsAwarded").asInt()).isZero();
        assertThat(body.at("/masteryChange/before").asDouble())
                .isEqualTo(body.at("/masteryChange/after").asDouble());
    }

    @Test
    void theQuestionOpensOnlyAfterEnoughOfTheVideoWasWatched() {
        CardView card = firstCard(null);

        assertError(get("/api/feed/cards/" + card.impressionId() + "/quiz"), 409, "VIDEO_NOT_WATCHED");
        assertError(get("/api/feed/cards/" + UUID.randomUUID() + "/quiz"), 404, "NOT_FOUND");
    }

    @Test
    void aCardWithoutUnansweredQuestionsAnswersNoContent() {
        CardView card = firstCard(null);
        watch(card);
        for (int question = 0; question < 3; question++) {
            List<JsonNode> quiz = openQuiz(card);
            if (quiz.isEmpty()) {
                return;
            }
            answer(quiz.getFirst(), "A", SLOW_ENOUGH_MS);
        }
        assertThat(openQuiz(card)).isEmpty();
    }

    @Test
    void tokensAreCheckedForSignatureOwnerQuestionAndAge() {
        CardView card = firstCard(null);
        watch(card);
        JsonNode quiz = openQuiz(card).getFirst();
        int questionId = quiz.get("microQuestionId").asInt();

        assertError(answerWith(questionId, quiz.get("quizToken").asString() + "x", "A"), 403, "QUIZ_TOKEN_INVALID");
        assertError(answerWith(questionId + 1, quiz.get("quizToken").asString(), "A"), 403, "QUIZ_TOKEN_INVALID");
        assertError(
                mvc.post()
                        .uri("/api/quiz/" + questionId + "/answer")
                        .header("Authorization", "Bearer " + login("deniz"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(quiz.get("quizToken").asString(), "A", SLOW_ENOUGH_MS))
                        .exchange(),
                403,
                "QUIZ_TOKEN_INVALID");

        QuizClaims claims = tokens.verify(quiz.get("quizToken").asString());
        QuizTokens past = new QuizTokens(properties, Clock.offset(Clock.systemUTC(), Duration.ofMinutes(-10)));
        assertError(answerWith(questionId, past.issue(claims), "A"), 410, "QUIZ_EXPIRED");
        assertError(answerWith(questionId, quiz.get("quizToken").asString(), "E"), 400, "INVALID_REQUEST");
    }

    @Test
    void tooManyAnswersInAMinuteAreRateLimited() {
        CardView card = firstCard(null);
        watch(card);
        JsonNode quiz = openQuiz(card).getFirst();
        for (int index = 0; index < properties.rateLimitPerMin(); index++) {
            jdbc.sql("""
                            insert into micro_quiz_attempt
                                (id, user_id, micro_question_id, impression_id, selected_key, is_correct, response_ms, points_awarded)
                            values (:id, :userId, :question, :impression, 'A', false, 5000, 0)
                            """)
                    .param("id", UUID.randomUUID())
                    .param("userId", userId)
                    .param("question", 10_000 + index)
                    .param("impression", UUID.randomUUID())
                    .update();
        }

        assertError(answer(quiz, "A", SLOW_ENOUGH_MS), 429, "RATE_LIMITED");
    }

    /** Mert after an exam with 3 wrong Olasılık answers, like Deniz in the demo. */
    private void weakOlasilik() {
        mastery.applyExamResult(new ExamResultPublished(
                UUID.randomUUID(),
                ExamResultPublished.SCHEMA_VERSION,
                Instant.now(),
                userId,
                UUID.randomUUID(),
                1,
                List.of(
                        new AnswerOutcome(106, PERKOM, "CORRECT", 30),
                        new AnswerOutcome(107, OLASILIK, "WRONG", 30),
                        new AnswerOutcome(109, OLASILIK, "WRONG", 30),
                        new AnswerOutcome(110, OLASILIK, "WRONG", 30))));
    }

    private CardView firstCard(Integer topicId) {
        return feed.page(userId, null, 1, topicId, true).cards().getFirst();
    }

    private void watch(CardView card) {
        interactions.record(
                userId,
                List.of(new Interaction(
                        "evt-" + UUID.randomUUID(),
                        card.impressionId(),
                        InteractionType.VIEW_COMPLETE,
                        card.content().durationSec() * 1000,
                        Instant.now())));
    }

    private List<JsonNode> openQuiz(CardView card) {
        MvcTestResult result = get("/api/feed/cards/" + card.impressionId() + "/quiz");
        if (result.getResponse().getStatus() == 204) {
            return List.of();
        }
        assertThat(result).hasStatusOk();
        JsonNode quiz = read(result);
        assertThat(quiz.toString()).doesNotContain("correctKey", "explanation");
        return List.of(quiz);
    }

    /** A second token for the same question, as a second browser tab would get. */
    private JsonNode reissue(JsonNode quiz) {
        QuizClaims claims = tokens.verify(quiz.get("quizToken").asString());
        QuizClaims twin = new QuizClaims(
                UUID.randomUUID(),
                claims.userId(),
                claims.impressionId(),
                claims.microQuestionId(),
                claims.shownToStored());
        return json.valueToTree(Map.of("microQuestionId", claims.microQuestionId(), "quizToken", tokens.issue(twin)));
    }

    private String correctShownKey(JsonNode quiz) {
        QuizClaims claims = tokens.verify(quiz.get("quizToken").asString());
        String stored =
                questions.question(claims.microQuestionId()).orElseThrow().correctKey();
        return claims.shownToStored().entrySet().stream()
                .filter(entry -> entry.getValue().equals(stored))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
    }

    private MvcTestResult answer(JsonNode quiz, String key, int responseMs) {
        return mvc.post()
                .uri("/api/quiz/" + quiz.get("microQuestionId").asInt() + "/answer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(quiz.get("quizToken").asString(), key, responseMs))
                .exchange();
    }

    private MvcTestResult answerWith(int questionId, String quizToken, String key) {
        return mvc.post()
                .uri("/api/quiz/" + questionId + "/answer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(quizToken, key, SLOW_ENOUGH_MS))
                .exchange();
    }

    private String body(String quizToken, String key, int responseMs) {
        return json.writeValueAsString(Map.of("quizToken", quizToken, "selectedKey", key, "responseMs", responseMs));
    }

    private MvcTestResult get(String uri) {
        return mvc.get().uri(uri).header("Authorization", "Bearer " + token).exchange();
    }

    private static void assertError(MvcTestResult result, int status, String code) {
        assertThat(result).hasStatus(status);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(code);
    }

    private String login(String slug) {
        return read(mvc.post()
                        .uri("/api/auth/demo-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"" + slug + "\"}")
                        .exchange())
                .get("token")
                .asString();
    }

    private JsonNode read(MvcTestResult result) {
        return json.readTree(result.getResponse().getContentAsByteArray());
    }
}
