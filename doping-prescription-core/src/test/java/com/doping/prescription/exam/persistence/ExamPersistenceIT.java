package com.doping.prescription.exam.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.exam.domain.AnswerResult;
import com.doping.prescription.exam.domain.AttemptScore;
import com.doping.prescription.exam.domain.AttemptStatus;
import com.doping.prescription.exam.domain.ExamOption;
import com.doping.prescription.exam.domain.ExamQuestionContent;
import com.doping.prescription.exam.domain.ExamType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

/** Commits real transactions (needed for the locking tests) and removes its own rows afterwards. */
@IntegrationTest
class ExamPersistenceIT {

    private static final Instant NOW = Instant.parse("2026-10-20T07:00:00Z");
    private static final AttemptScore SCORE = new AttemptScore(8, 3, 1, new BigDecimal("7.25"));

    @Autowired
    private ExamRepository exams;

    @Autowired
    private ExamQuestionRepository questions;

    @Autowired
    private ExamAttemptRepository attempts;

    @Autowired
    private ExamAnswerRepository answers;

    @Autowired
    private TransactionTemplate transaction;

    private final List<UUID> createdAttempts = new ArrayList<>();
    private Exam exam;

    @BeforeEach
    void createExam() {
        exam = exams.save(new Exam("Test Denemesi", ExamType.TYT, 20, NOW));
    }

    @AfterEach
    void removeCreatedRows() {
        answers.deleteAll(answers.findAll().stream()
                .filter(answer -> createdAttempts.contains(answer.getId().attemptId()))
                .toList());
        attempts.deleteAllById(createdAttempts);
        questions.deleteAll(questions.findAll().stream()
                .filter(question -> question.getExamId().equals(exam.getId()))
                .toList());
        exams.delete(exam);
    }

    @Test
    void storesQuestionWithOptionsAndScenarioAnswerAndGradedAnswer() {
        // Given
        List<ExamOption> options = List.of(
                new ExamOption("A", "1"),
                new ExamOption("B", "2"),
                new ExamOption("C", "3"),
                new ExamOption("D", "4"),
                new ExamOption("E", "5"));
        ExamQuestion question = questions.save(new ExamQuestion(
                exam.getId(), 1, 9, new ExamQuestionContent("Soru", options, "C", "Çözüm", 2), null, NOW));
        ExamAttempt attempt = startAttempt(UUID.randomUUID());
        ExamAnswer answer = new ExamAnswer(new ExamAnswerId(attempt.getId(), question.getId()), "A", 42, NOW);
        answer.grade(AnswerResult.WRONG, NOW);
        answers.save(answer);

        // When
        ExamQuestion loadedQuestion = questions.findById(question.getId()).orElseThrow();
        ExamAnswer loadedAnswer = answers.findById(answer.getId()).orElseThrow();

        // Then
        assertThat(loadedQuestion.getContent().options()).containsExactlyElementsOf(options);
        assertThat(loadedQuestion.getScenarioAnswer()).isEmpty();
        assertThat(loadedAnswer.getSelectedKey()).contains("A");
        assertThat(loadedAnswer.getResult()).contains(AnswerResult.WRONG);
    }

    @Test
    void rejectsSecondInProgressAttemptForSameLearnerAndExam() {
        // Given
        UUID learner = UUID.randomUUID();
        startAttempt(learner);

        // When / Then
        assertThatThrownBy(() -> startAttempt(learner)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsNewAttemptOnceThePreviousOneIsSubmitted() {
        // Given
        UUID learner = UUID.randomUUID();
        ExamAttempt first = startAttempt(learner);
        transaction.executeWithoutResult(status -> {
            ExamAttempt loaded = attempts.findById(first.getId()).orElseThrow();
            loaded.submit(SCORE, NOW);
        });

        // When
        ExamAttempt second = startAttempt(learner);

        // Then
        assertThat(attempts.findById(first.getId()).orElseThrow().getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(attempts.findById(first.getId()).orElseThrow().getScore()).contains(SCORE);
        assertThat(second.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
    }

    @Test
    void secondConcurrentSubmitOfSameAttemptFails() {
        // Given: two requests loaded the same attempt before either submitted
        ExamAttempt attempt = startAttempt(UUID.randomUUID());
        ExamAttempt firstCopy = attempts.findById(attempt.getId()).orElseThrow();
        ExamAttempt secondCopy = attempts.findById(attempt.getId()).orElseThrow();
        firstCopy.submit(SCORE, NOW);
        secondCopy.submit(SCORE, NOW);
        attempts.save(firstCopy);

        // When / Then
        assertThatThrownBy(() -> attempts.save(secondCopy)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    private ExamAttempt startAttempt(UUID learner) {
        ExamAttempt attempt = new ExamAttempt(UUID.randomUUID(), learner, exam.getId(), NOW);
        createdAttempts.add(attempt.getId());
        return attempts.saveAndFlush(attempt);
    }
}
