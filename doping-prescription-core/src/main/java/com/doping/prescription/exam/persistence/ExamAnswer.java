package com.doping.prescription.exam.persistence;

import com.doping.prescription.exam.domain.AnswerResult;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Persistable;

/** The learner's answer to one question of an attempt; an empty selection means the question was left blank. */
@Entity
public class ExamAnswer implements Persistable<ExamAnswerId> {

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

    /** The id is assigned by the application, so Spring Data cannot tell a new row by it; save() then inserts directly. */
    @Transient
    private boolean newRow = true;

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

    // Persistable declares the id nullable for unsaved entities; here it is assigned in the constructor
    @Override
    public @NonNull ExamAnswerId getId() {
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
