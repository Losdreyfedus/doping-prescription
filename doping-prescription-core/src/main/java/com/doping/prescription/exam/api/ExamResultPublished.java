package com.doping.prescription.exam.api;

import com.doping.prescription.shared.api.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

/**
 * A learner submitted an exam (docs/api.md, exam.result-published). Written to the outbox in the submit transaction and
 * sent to Kafka after commit, keyed by user id. Carries results per question but never the answer key.
 *
 * @param answers one entry per question, in exam order
 */
@Externalized(ExamResultPublished.TOPIC + "::#{userId().toString()}")
public record ExamResultPublished(
        UUID eventId,
        int schemaVersion,
        Instant occurredAt,
        UUID userId,
        UUID attemptId,
        Integer examId,
        List<AnswerOutcome> answers)
        implements DomainEvent {

    public static final String TOPIC = "exam.result-published";
    public static final int SCHEMA_VERSION = 1;

    public ExamResultPublished {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(attemptId, "attemptId");
        answers = List.copyOf(answers);
    }

    /**
     * @param result       {@code CORRECT}, {@code WRONG} or {@code BLANK}
     * @param timeSpentSec seconds spent on the question, if the client reported it
     */
    public record AnswerOutcome(Integer questionId, Integer topicId, String result, Integer timeSpentSec) {}
}
