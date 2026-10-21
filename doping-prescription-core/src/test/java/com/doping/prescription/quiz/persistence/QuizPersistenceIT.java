package com.doping.prescription.quiz.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.quiz.domain.GradedAnswer;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class QuizPersistenceIT {

    private static final int MICRO_QUESTION = 37;

    @Autowired
    private MicroQuizAttemptRepository attempts;

    private final UUID learner = UUID.randomUUID();

    @Test
    void storesGradedAnswer() {
        // Given
        MicroQuizAttempt attempt = attempts.saveAndFlush(answer(new GradedAnswer("B", true, 14_300), 15));

        // When
        MicroQuizAttempt loaded = attempts.findById(attempt.getId()).orElseThrow();

        // Then
        assertThat(loaded.getAnswer()).isEqualTo(new GradedAnswer("B", true, 14_300));
        assertThat(loaded.getPointsAwarded()).isEqualTo(15);
    }

    @Test
    void rejectsSecondAnswerToSameQuestion() {
        // Given
        attempts.saveAndFlush(answer(new GradedAnswer("B", true, 14_300), 15));

        // When / Then
        assertThatThrownBy(() -> attempts.saveAndFlush(answer(new GradedAnswer("A", false, 9_000), 0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private MicroQuizAttempt answer(GradedAnswer gradedAnswer, int points) {
        return new MicroQuizAttempt(
                UUID.randomUUID(), learner, MICRO_QUESTION, UUID.randomUUID(), gradedAnswer, points);
    }
}
