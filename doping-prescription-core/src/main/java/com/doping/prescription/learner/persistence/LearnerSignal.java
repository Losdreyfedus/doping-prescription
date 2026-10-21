package com.doping.prescription.learner.persistence;

import com.doping.prescription.learner.domain.SignalType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One immutable entry of the learner's signal log. */
@Entity
public class LearnerSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID userId;

    private Integer topicId;

    @Enumerated(EnumType.STRING)
    private SignalType signalType;

    private String sourceRef;

    private Instant occurredAt;

    private String idempotencyKey;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    protected LearnerSignal() {
        // for JPA
    }

    public LearnerSignal(
            UUID userId,
            Integer topicId,
            SignalType signalType,
            String sourceRef,
            Instant occurredAt,
            String idempotencyKey) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.topicId = Objects.requireNonNull(topicId, "topicId");
        this.signalType = Objects.requireNonNull(signalType, "signalType");
        this.sourceRef = Objects.requireNonNull(sourceRef, "sourceRef");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
    }

    public Long getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Integer getTopicId() {
        return topicId;
    }

    public SignalType getSignalType() {
        return signalType;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
