package com.doping.prescription.points.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A learner's streak of consecutive correct micro-question answers. Applying the same answer ({@code refId}) twice has
 * no effect; the version column turns concurrent updates into a retryable conflict.
 */
@Entity
public class PointsStreak {

    @Id
    private UUID userId;

    @Column(name = "current")
    private int currentStreak;

    @Column(name = "best")
    private int bestStreak;

    private String lastRefId;

    @Version
    private long version;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected PointsStreak() {
        // for JPA
    }

    public PointsStreak(UUID userId, Instant now) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    /** A correct answer extends the streak, a wrong one resets it. */
    public void apply(String refId, boolean correct, Instant now) {
        Objects.requireNonNull(refId, "refId");
        if (refId.equals(lastRefId)) {
            return;
        }
        currentStreak = correct ? currentStreak + 1 : 0;
        bestStreak = Math.max(bestStreak, currentStreak);
        lastRefId = refId;
        updatedAt = now;
    }

    public UUID getUserId() {
        return userId;
    }

    public int getCurrent() {
        return currentStreak;
    }

    public int getBest() {
        return bestStreak;
    }
}
