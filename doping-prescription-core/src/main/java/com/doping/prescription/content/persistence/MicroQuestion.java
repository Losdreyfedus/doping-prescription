package com.doping.prescription.content.persistence;

import com.doping.prescription.content.domain.AnswerOption;
import com.doping.prescription.content.domain.MicroQuestionStatus;
import com.doping.prescription.content.domain.QuestionContent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A one-question check shown after a video. {@code correctKey} never leaves the server. */
@Entity
public class MicroQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer contentId;

    private Integer topicId;

    private String stem;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<AnswerOption> options;

    @JdbcTypeCode(SqlTypes.CHAR)
    private String correctKey;

    private String explanation;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int difficulty;

    @Enumerated(EnumType.STRING)
    private MicroQuestionStatus status;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected MicroQuestion() {
        // for JPA
    }

    public MicroQuestion(Integer contentId, Integer topicId, QuestionContent content, Instant now) {
        this.contentId = Objects.requireNonNull(contentId, "contentId");
        this.topicId = Objects.requireNonNull(topicId, "topicId");
        Objects.requireNonNull(content, "content");
        this.stem = content.stem();
        this.options = content.options();
        this.correctKey = content.correctKey();
        this.explanation = content.explanation();
        this.difficulty = content.difficulty();
        this.status = MicroQuestionStatus.LIVE;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public Integer getId() {
        return id;
    }

    public Integer getContentId() {
        return contentId;
    }

    public Integer getTopicId() {
        return topicId;
    }

    public QuestionContent getContent() {
        return new QuestionContent(stem, options, correctKey, explanation, difficulty);
    }

    public MicroQuestionStatus getStatus() {
        return status;
    }
}
