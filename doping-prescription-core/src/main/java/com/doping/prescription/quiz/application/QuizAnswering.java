package com.doping.prescription.quiz.application;

import com.doping.prescription.learner.api.LearnerSignalRecorder;
import com.doping.prescription.learner.api.LearnerSignalRecorder.MasteryChange;
import com.doping.prescription.points.api.PointsLedger;
import com.doping.prescription.points.api.PointsLedger.Award;
import com.doping.prescription.quiz.api.QuizAnswered;
import com.doping.prescription.quiz.domain.AnswerPolicy;
import com.doping.prescription.quiz.domain.GradedAnswer;
import com.doping.prescription.quiz.persistence.MicroQuizAttempt;
import com.doping.prescription.quiz.persistence.MicroQuizAttemptRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single transaction of an answer (docs/api.md, POST /api/quiz/:id/answer): claim the answer, record the signal,
 * award the points, publish the event. A duplicate answer fails at the claim with {@link AlreadyAnswered} and rolls
 * everything back.
 */
@Component
class QuizAnswering {

    private final MicroQuizAttemptRepository attempts;
    private final LearnerSignalRecorder signals;
    private final PointsLedger points;
    private final ApplicationEventPublisher events;
    private final AnswerPolicy policy;
    private final Clock clock;

    QuizAnswering(
            MicroQuizAttemptRepository attempts,
            LearnerSignalRecorder signals,
            PointsLedger points,
            ApplicationEventPublisher events,
            QuizProperties properties,
            Clock clock) {
        this.attempts = attempts;
        this.signals = signals;
        this.points = points;
        this.events = events;
        this.policy = new AnswerPolicy(properties.minResponseMs());
        this.clock = clock;
    }

    /**
     * @throws AlreadyAnswered if the learner already answered the question
     */
    @Transactional
    public Outcome answerOnce(Answer answer) {
        Instant now = clock.instant();
        UUID attemptId = UUID.randomUUID();
        MicroQuizAttempt attempt = claim(attemptId, answer);

        Outcome outcome;
        if (!policy.counts(answer.responseMs())) {
            // too fast to have read the question: no signal, no points (docs/algoritma.md §1)
            outcome = unchanged(answer.userId(), answer.topicId());
        } else {
            MasteryChange change = signals.recordQuizAnswer(
                    answer.userId(), answer.topicId(), answer.correct(), attemptId.toString(), now);
            Award award = points.award(answer.userId(), attemptId.toString(), answer.correct(), answer.weakTopic());
            outcome = new Outcome(change, award);
        }
        attempt.recordPoints(outcome.award().pointsAwarded());
        events.publishEvent(new QuizAnswered(
                UUID.randomUUID(),
                QuizAnswered.SCHEMA_VERSION,
                now,
                answer.userId(),
                attemptId,
                answer.impressionId(),
                answer.microQuestionId(),
                answer.topicId(),
                answer.correct(),
                answer.responseMs()));
        return outcome;
    }

    private MicroQuizAttempt claim(UUID attemptId, Answer answer) {
        try {
            return attempts.saveAndFlush(new MicroQuizAttempt(
                    attemptId,
                    answer.userId(),
                    answer.microQuestionId(),
                    answer.impressionId(),
                    new GradedAnswer(answer.storedKey(), answer.correct(), answer.responseMs()),
                    0));
        } catch (DataIntegrityViolationException failure) {
            if (MicroQuizAttempt.isSecondAnswer(failure)) {
                throw new AlreadyAnswered(failure);
            }
            throw failure;
        }
    }

    /** The learner's current state, for answers that change nothing. */
    @Transactional(readOnly = true)
    public Outcome unchanged(UUID userId, Integer topicId) {
        return new Outcome(signals.unchanged(userId, topicId), points.current(userId));
    }

    /**
     * @param storedKey the chosen option's stored letter (shuffle undone)
     * @param weakTopic whether the card came from the WEAK_TOPIC pool
     */
    record Answer(
            UUID userId,
            Integer microQuestionId,
            UUID impressionId,
            Integer topicId,
            String storedKey,
            boolean correct,
            int responseMs,
            boolean weakTopic) {}

    record Outcome(MasteryChange change, Award award) {}

    /** The learner already answered the question; the answer earns nothing (docs/api.md). */
    static final class AlreadyAnswered extends RuntimeException {

        private static final long serialVersionUID = 1L;

        AlreadyAnswered(Throwable cause) {
            super("Micro-question already answered", cause);
        }
    }
}
