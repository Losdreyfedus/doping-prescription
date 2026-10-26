package com.doping.prescription.quiz.persistence;

import com.doping.prescription.quiz.domain.GradedAnswer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Persistable;

/** A learner's answer to a micro-question. At most one per learner and question (unique constraint). */
@Entity
public class MicroQuizAttempt implements Persistable<UUID> {

    private static final String ONE_ANSWER_PER_QUESTION = "uq_micro_quiz_attempt_user_question";

    @Id
    private UUID id;

    private UUID userId;

    private Integer microQuestionId;

    private UUID impressionId;

    @JdbcTypeCode(SqlTypes.CHAR)
    private String selectedKey;

    @Column(name = "is_correct")
    private boolean correct;

    private int responseMs;

    private int pointsAwarded;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    /** Whether the insert failed because the learner already answered the question, not for any other reason. */
    public static boolean isSecondAnswer(DataIntegrityViolationException failure) {
        return failure.getCause() instanceof ConstraintViolationException violation
                && ONE_ANSWER_PER_QUESTION.equalsIgnoreCase(violation.getConstraintName());
    }

    /** The id is assigned by the application, so Spring Data cannot tell a new row by it; save() then inserts directly. */
    @Transient
    private boolean newRow = true;

    protected MicroQuizAttempt() {
        // for JPA
    }

    public MicroQuizAttempt(
            UUID id, UUID userId, Integer microQuestionId, UUID impressionId, GradedAnswer answer, int pointsAwarded) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.microQuestionId = Objects.requireNonNull(microQuestionId, "microQuestionId");
        this.impressionId = Objects.requireNonNull(impressionId, "impressionId");
        Objects.requireNonNull(answer, "answer");
        this.selectedKey = answer.selectedKey();
        this.correct = answer.correct();
        this.responseMs = answer.responseMs();
        this.pointsAwarded = pointsAwarded;
    }

    /** Points are known only after the answer was claimed (inserted) and the ledger applied it. */
    public void recordPoints(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("points must not be negative");
        }
        this.pointsAwarded = points;
    }

    // Persistable declares the id nullable for unsaved entities; here it is assigned in the constructor
    @Override
    public @NonNull UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return newRow;
    }

    @PostLoad
    @PostPersist
    void markStored() {
        newRow = false;
    }

    public UUID getUserId() {
        return userId;
    }

    public Integer getMicroQuestionId() {
        return microQuestionId;
    }

    public UUID getImpressionId() {
        return impressionId;
    }

    public GradedAnswer getAnswer() {
        return new GradedAnswer(selectedKey, correct, responseMs);
    }

    public int getPointsAwarded() {
        return pointsAwarded;
    }
}
