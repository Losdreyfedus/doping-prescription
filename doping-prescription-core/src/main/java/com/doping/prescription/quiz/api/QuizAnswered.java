package com.doping.prescription.quiz.api;

import com.doping.prescription.shared.api.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

/**
 * A micro-question was answered (docs/api.md, quiz.answered). Published for analytics; the learner does not listen
 * because its signal was already written synchronously. Never carries the answer key.
 */
@Externalized(QuizAnswered.TOPIC + "::#{userId().toString()}")
public record QuizAnswered(
        UUID eventId,
        int schemaVersion,
        Instant occurredAt,
        UUID userId,
        UUID quizAttemptId,
        UUID impressionId,
        Integer microQuestionId,
        Integer topicId,
        boolean isCorrect,
        int responseMs)
        implements DomainEvent {

    public static final String TOPIC = "quiz.answered";
    public static final int SCHEMA_VERSION = 1;

    public QuizAnswered {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(userId, "userId");
    }
}
