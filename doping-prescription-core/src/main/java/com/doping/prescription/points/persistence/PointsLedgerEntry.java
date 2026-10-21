package com.doping.prescription.points.persistence;

import com.doping.prescription.points.domain.PointsReason;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One award in the append-only points ledger; {@code refId} makes each award idempotent. */
@Entity
@Table(name = "points_ledger")
public class PointsLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID userId;

    private int amount;

    @Enumerated(EnumType.STRING)
    private PointsReason reason;

    private String refId;

    /** From the application Clock, so the daily cap uses the same calendar day as the rest of the code. */
    private Instant createdAt;

    protected PointsLedgerEntry() {
        // for JPA
    }

    public PointsLedgerEntry(UUID userId, int amount, PointsReason reason, String refId, Instant awardedAt) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        this.userId = Objects.requireNonNull(userId, "userId");
        this.amount = amount;
        this.reason = Objects.requireNonNull(reason, "reason");
        this.refId = Objects.requireNonNull(refId, "refId");
        this.createdAt = Objects.requireNonNull(awardedAt, "awardedAt");
    }

    public Long getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public int getAmount() {
        return amount;
    }

    public PointsReason getReason() {
        return reason;
    }

    public String getRefId() {
        return refId;
    }

    public Instant getAwardedAt() {
        return createdAt;
    }
}
