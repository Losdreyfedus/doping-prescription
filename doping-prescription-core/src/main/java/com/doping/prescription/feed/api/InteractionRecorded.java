package com.doping.prescription.feed.api;

import com.doping.prescription.shared.api.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.modulith.events.Externalized;

/**
 * A learner interacted with a feed card (docs/api.md, feed.interaction-recorded).
 *
 * @param topicId     the card's target topic
 * @param interaction VIEW_START, VIEW_COMPLETE, SKIP, LIKE, QUIZ_SHOWN or QUIZ_DISMISSED
 * @param watchMs     real video milliseconds watched, if reported
 */
@Externalized(InteractionRecorded.TOPIC + "::#{userId().toString()}")
public record InteractionRecorded(
        UUID eventId,
        int schemaVersion,
        Instant occurredAt,
        UUID userId,
        UUID impressionId,
        Integer contentId,
        Integer topicId,
        String interaction,
        @Nullable Integer watchMs)
        implements DomainEvent {

    public static final String TOPIC = "feed.interaction-recorded";
    public static final int SCHEMA_VERSION = 1;

    public InteractionRecorded {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(impressionId, "impressionId");
    }
}
