package com.doping.prescription.exam.persistence;

import com.doping.prescription.exam.domain.AnswerResult;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** The learner's answer to one question of an attempt; an empty selection means the question was left blank. */
@Entity
public class ExamAnswer {

    @EmbeddedId
    private ExamAnswerId id;

    @JdbcTypeCode(SqlTypes.CHAR)
    private String selectedKey;

    @Enumerated(EnumType.STRING)
    private AnswerResult result;

    private Integer timeSpentSec;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected ExamAnswer() {
        // for JPA
    }

    public ExamAnswer(ExamAnswerId id, String selectedKey, Integer timeSpentSec, Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        this.selectedKey = selectedKey;
        this.timeSpentSec = timeSpentSec;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    /** The learner changed or cleared the answer while the attempt is in progress. */
    public void change(String newSelectedKey, Integer newTimeSpentSec, Instant now) {
        this.selectedKey = newSelectedKey;
        this.timeSpentSec = newTimeSpentSec;
        this.updatedAt = now;
    }

    public void grade(AnswerResult result, Instant now) {
        this.result = Objects.requireNonNull(result, "result");
        this.updatedAt = now;
    }

    public ExamAnswerId getId() {
        return id;
    }

    public Optional<String> getSelectedKey() {
        return Optional.ofNullable(selectedKey);
    }

    public Optional<AnswerResult> getResult() {
        return Optional.ofNullable(result);
    }

    public Optional<Integer> getTimeSpentSec() {
        return Optional.ofNullable(timeSpentSec);
    }
}
