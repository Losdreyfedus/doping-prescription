package com.doping.prescription.learner.api;

import com.doping.prescription.shared.api.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

/**
 * A learner's mastery moved by at least {@code prescription.learner.mastery.change-event-threshold} in one or more
 * topics (docs/api.md, learner.mastery-changed). The feed drops the learner's queued cards when it sees this.
 *
 * @param trigger the signal kind that caused the change, e.g. EXAM_RESULT or QUIZ_CORRECT
 */
@Externalized(MasteryChanged.TOPIC + "::#{userId().toString()}")
public record MasteryChanged(
        UUID eventId, int schemaVersion, Instant occurredAt, UUID userId, String trigger, List<TopicChange> changes)
        implements DomainEvent {

    public static final String TOPIC = "learner.mastery-changed";
    public static final int SCHEMA_VERSION = 1;

    public MasteryChanged {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(userId, "userId");
        changes = List.copyOf(changes);
    }

    /** One topic's mastery before and after, rounded like the API (docs/algoritma.md §1). */
    public record TopicChange(Integer topicId, double before, double after, double confidence) {}
}
