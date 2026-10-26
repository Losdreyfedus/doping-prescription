package com.doping.prescription.points.persistence;

import com.doping.prescription.points.domain.Streak;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A learner's streak of consecutive correct micro-question answers. Applying the same answer ({@code refId}) twice has
 * no effect. Answers lock the row while they apply; the version column turns any other concurrent update into a
 * conflict the answer retries.
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

    // A wrapper type: null marks a new row, so save() inserts without first selecting it
    @Version
    private Long version;

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

    /** Stores the answer's effect by the rule of {@link Streak#apply}. */
    public void apply(String refId, boolean correct, Instant now) {
        Streak before = streak();
        Streak after = before.apply(refId, correct);
        if (after.equals(before)) {
            return;
        }
        currentStreak = after.current();
        bestStreak = after.best();
        lastRefId = after.lastRefId();
        updatedAt = now;
    }

    public Streak streak() {
        return new Streak(currentStreak, bestStreak, lastRefId);
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
