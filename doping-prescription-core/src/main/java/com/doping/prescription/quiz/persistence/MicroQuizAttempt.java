package com.doping.prescription.quiz.persistence;

import com.doping.prescription.quiz.domain.GradedAnswer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A learner's answer to a micro-question. At most one per learner and question (unique constraint). */
@Entity
public class MicroQuizAttempt {

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

    public UUID getId() {
        return id;
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
