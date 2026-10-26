package com.doping.prescription.learner.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A learner's mastery of one topic at full precision (docs/algoritma.md §1). The values themselves are computed by the
 * domain; this entity only stores them. The version column turns concurrent updates into a retryable conflict.
 */
@Entity
@Table(name = "learner_topic_mastery")
public class TopicMastery {

    @EmbeddedId
    private TopicMasteryId id;

    private double mastery;

    private double confidence;

    private int attempts;

    private int correct;

    private Instant lastSignalAt;

    private Instant lastServedAt;

    private UUID lastExamAttemptId;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer lastExamTotal;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer lastExamCorrect;

    // A wrapper type: null marks a new row, so save() inserts without first selecting it
    @Version
    private Long version;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected TopicMastery() {
        // for JPA
    }

    public TopicMastery(TopicMasteryId id, double initialMastery, Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        this.mastery = initialMastery;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    /** Stores the result of one mastery-affecting signal. */
    public void recordSignal(double newMastery, double newConfidence, boolean wasCorrect, Instant signalAt) {
        this.mastery = newMastery;
        this.confidence = newConfidence;
        this.attempts++;
        if (wasCorrect) {
            this.correct++;
        }
        this.lastSignalAt = signalAt;
        this.updatedAt = signalAt;
    }

    /** Remembers how the learner did on this topic in their latest exam, for the why-this-video template. */
    public void recordExamSummary(LastExamSummary summary, Instant now) {
        this.lastExamAttemptId = summary.attemptId();
        this.lastExamTotal = summary.total();
        this.lastExamCorrect = summary.correct();
        this.updatedAt = now;
    }

    /** The feed served a video of this topic; kept for future pools, not part of mastery. */
    public void recordServed(Instant servedAt) {
        if (lastServedAt == null || servedAt.isAfter(lastServedAt)) {
            this.lastServedAt = servedAt;
            this.updatedAt = servedAt;
        }
    }

    public Optional<LastExamSummary> getLastExamSummary() {
        if (lastExamAttemptId == null) {
            return Optional.empty();
        }
        return Optional.of(new LastExamSummary(lastExamAttemptId, lastExamTotal, lastExamCorrect));
    }

    public TopicMasteryId getId() {
        return id;
    }

    public UUID getUserId() {
        return id.userId();
    }

    public Integer getTopicId() {
        return id.topicId();
    }

    public double getMastery() {
        return mastery;
    }

    public double getConfidence() {
        return confidence;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getCorrect() {
        return correct;
    }

    public Optional<Instant> getLastSignalAt() {
        return Optional.ofNullable(lastSignalAt);
    }

    public Optional<Instant> getLastServedAt() {
        return Optional.ofNullable(lastServedAt);
    }
}
