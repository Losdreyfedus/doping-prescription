package com.doping.prescription.exam.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.auth.seed.AuthDemoDataLoader;
import com.doping.prescription.content.seed.ContentDemoDataLoader;
import com.doping.prescription.exam.application.ExamViews.AttemptView;
import com.doping.prescription.exam.application.ExamViews.ExamResult;
import com.doping.prescription.exam.seed.ExamDemoDataLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Concurrent requests on one attempt (docs/kod-standartlari.md §7, "Eşzamanlı isteklerde çift kayıt"): two submits
 * publish the result once and the second answers 409; two starts share one attempt.
 */
@IntegrationTest
class ExamServiceIT {

    private static final String SLUG = "selin";
    private static final int MINI_EXAM_1 = 1;
    private static final int FIRST_QUESTION = 101;

    @Autowired
    private ExamService exams;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private AuthDemoDataLoader users;

    @Autowired
    private ContentDemoDataLoader content;

    @Autowired
    private ExamDemoDataLoader examSeed;

    private UUID userId;

    @BeforeEach
    void freshLearner() {
        users.load();
        content.load();
        examSeed.load();
        userId = jdbc.sql("select id from users where slug = :slug")
                .param("slug", SLUG)
                .query(UUID.class)
                .single();
        jdbc.sql("delete from exam_answer where attempt_id in (select id from exam_attempt where user_id = :userId)")
                .param("userId", userId)
                .update();
        jdbc.sql("delete from exam_attempt where user_id = :userId")
                .param("userId", userId)
                .update();
    }

    @Test
    void twoConcurrentSubmitsGradeOnceAndTheSecondIsAlreadySubmitted() throws Exception {
        // Given
        UUID attemptId = exams.startOrResume(userId, MINI_EXAM_1).attemptId();
        exams.saveAnswer(userId, attemptId, FIRST_QUESTION, "B", 30);

        // When
        List<Object> outcomes =
                concurrently(() -> exams.submit(userId, attemptId), () -> exams.submit(userId, attemptId));

        // Then
        assertThat(outcomes).hasSize(2);
        assertThat(outcomes).filteredOn(ExamResult.class::isInstance).hasSize(1);
        assertThat(outcomes)
                .filteredOn(AttemptAlreadySubmittedException.class::isInstance)
                .hasSize(1);
        // The losing submit rolls back with its event, so the result is published once; ExamSubmissionIT proves it
        // against real Kafka (this context runs without event externalization).
    }

    @Test
    void twoConcurrentStartsShareOneAttempt() throws Exception {
        // When
        List<Object> outcomes = concurrently(
                () -> exams.startOrResume(userId, MINI_EXAM_1), () -> exams.startOrResume(userId, MINI_EXAM_1));

        // Then
        assertThat(outcomes).allSatisfy(outcome -> assertThat(outcome).isInstanceOf(AttemptView.class));
        assertThat(outcomes)
                .extracting(outcome -> ((AttemptView) outcome).attemptId())
                .containsOnly(((AttemptView) outcomes.getFirst()).attemptId());
        assertThat(jdbc.sql("select count(*) from exam_attempt where user_id = :userId")
                        .param("userId", userId)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
    }

    @Test
    void anAnswerAfterTheSubmitIsRejected() {
        // Given
        UUID attemptId = exams.startOrResume(userId, MINI_EXAM_1).attemptId();
        exams.submit(userId, attemptId);

        // When
        Throwable failure = catchThrowable(() -> exams.saveAnswer(userId, attemptId, FIRST_QUESTION, "B", 30));

        // Then
        assertThat(failure).isInstanceOf(AttemptAlreadySubmittedException.class);
    }

    /** Runs both calls at the same moment; each outcome is the call's result or the exception it threw. */
    private static List<Object> concurrently(Callable<?> one, Callable<?> other) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Callable<?> call : List.of(one, other)) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return call.call();
                }));
            }
            start.countDown();
            List<Object> outcomes = new ArrayList<>();
            for (Future<?> future : futures) {
                try {
                    outcomes.add(future.get());
                } catch (ExecutionException failed) {
                    outcomes.add(failed.getCause());
                }
            }
            return outcomes;
        } finally {
            pool.shutdown();
        }
    }
}
