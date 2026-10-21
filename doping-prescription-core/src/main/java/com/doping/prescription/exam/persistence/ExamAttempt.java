package com.doping.prescription.exam.persistence;

import com.doping.prescription.exam.domain.AttemptScore;
import com.doping.prescription.exam.domain.AttemptStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A learner's attempt at an exam. The database allows one IN_PROGRESS attempt per learner and exam; the version column
 * makes a second concurrent submit fail instead of publishing the result twice.
 */
@Entity
public class ExamAttempt {

    @Id
    private UUID id;

    private UUID userId;

    private Integer examId;

    @Enumerated(EnumType.STRING)
    private AttemptStatus status;

    private Instant startedAt;

    private Instant submittedAt;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer correctCount;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer wrongCount;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer blankCount;

    private BigDecimal net;

    @Version
    private long version;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected ExamAttempt() {
        // for JPA
    }

    public ExamAttempt(UUID id, UUID userId, Integer examId, Instant startedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.examId = Objects.requireNonNull(examId, "examId");
        this.status = AttemptStatus.IN_PROGRESS;
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.updatedAt = startedAt;
    }

    /** Records the totals and closes the attempt; an attempt can be submitted only once. */
    public void submit(AttemptScore score, Instant now) {
        if (status == AttemptStatus.SUBMITTED) {
            throw new IllegalStateException("Attempt " + id + " is already submitted");
        }
        this.correctCount = score.correct();
        this.wrongCount = score.wrong();
        this.blankCount = score.blank();
        this.net = score.net();
        this.status = AttemptStatus.SUBMITTED;
        this.submittedAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Integer getExamId() {
        return examId;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Optional<Instant> getSubmittedAt() {
        return Optional.ofNullable(submittedAt);
    }

    public Optional<AttemptScore> getScore() {
        if (status != AttemptStatus.SUBMITTED) {
            return Optional.empty();
        }
        return Optional.of(new AttemptScore(correctCount, wrongCount, blankCount, net));
    }
}
