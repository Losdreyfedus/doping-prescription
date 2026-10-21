package com.doping.prescription.feed.persistence;

import com.doping.prescription.feed.domain.InteractionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** One interaction event sent by the web client; {@code clientEventId} makes retried batches idempotent. */
@Entity
public class FeedInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID impressionId;

    private UUID userId;

    private String clientEventId;

    @Enumerated(EnumType.STRING)
    private InteractionType event;

    private Integer watchMs;

    private Instant clientTs;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    protected FeedInteraction() {
        // for JPA
    }

    public FeedInteraction(
            UUID impressionId,
            UUID userId,
            String clientEventId,
            InteractionType event,
            Integer watchMs,
            Instant clientTs) {
        this.impressionId = Objects.requireNonNull(impressionId, "impressionId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.clientEventId = Objects.requireNonNull(clientEventId, "clientEventId");
        this.event = Objects.requireNonNull(event, "event");
        this.watchMs = watchMs;
        this.clientTs = Objects.requireNonNull(clientTs, "clientTs");
    }

    public Long getId() {
        return id;
    }

    public UUID getImpressionId() {
        return impressionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getClientEventId() {
        return clientEventId;
    }

    public InteractionType getEvent() {
        return event;
    }

    public Optional<Integer> getWatchMs() {
        return Optional.ofNullable(watchMs);
    }

    public Instant getClientTs() {
        return clientTs;
    }
}
