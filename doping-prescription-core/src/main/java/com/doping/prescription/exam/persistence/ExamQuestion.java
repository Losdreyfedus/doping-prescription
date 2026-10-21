package com.doping.prescription.exam.persistence;

import com.doping.prescription.exam.domain.ExamOption;
import com.doping.prescription.exam.domain.ExamQuestionContent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A question of an exam. {@code correctKey} is revealed only in a submitted attempt's result; {@code scenarioAnswer} is
 * demo data for the admin answer key and personas and is never sent to students.
 */
@Entity
public class ExamQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer examId;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int orderNo;

    private Integer topicId;

    private String stem;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<ExamOption> options;

    @JdbcTypeCode(SqlTypes.CHAR)
    private String correctKey;

    private String explanation;

    @JdbcTypeCode(SqlTypes.CHAR)
    private String scenarioAnswer;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int difficulty;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected ExamQuestion() {
        // for JPA
    }

    public ExamQuestion(
            Integer examId,
            int orderNo,
            Integer topicId,
            ExamQuestionContent content,
            String scenarioAnswer,
            Instant now) {
        this.examId = Objects.requireNonNull(examId, "examId");
        this.orderNo = orderNo;
        this.topicId = Objects.requireNonNull(topicId, "topicId");
        Objects.requireNonNull(content, "content");
        this.stem = content.stem();
        this.options = content.options();
        this.correctKey = content.correctKey();
        this.explanation = content.explanation();
        this.difficulty = content.difficulty();
        this.scenarioAnswer = scenarioAnswer;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public Integer getId() {
        return id;
    }

    public Integer getExamId() {
        return examId;
    }

    public int getOrderNo() {
        return orderNo;
    }

    public Integer getTopicId() {
        return topicId;
    }

    public ExamQuestionContent getContent() {
        return new ExamQuestionContent(stem, options, correctKey, explanation, difficulty);
    }

    public Optional<String> getScenarioAnswer() {
        return Optional.ofNullable(scenarioAnswer);
    }
}
