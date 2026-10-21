package com.doping.prescription.content.persistence;

import com.doping.prescription.content.domain.ExamType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** A curriculum topic: the shared language that links exam questions, videos and micro-questions. */
@Entity
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String code;

    private String name;

    @Enumerated(EnumType.STRING)
    private ExamType examType;

    private String subject;

    private BigDecimal examWeight;

    private int sortOrder;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected Topic() {
        // for JPA
    }

    public Topic(
            String code,
            String name,
            ExamType examType,
            String subject,
            BigDecimal examWeight,
            int sortOrder,
            Instant now) {
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.examType = Objects.requireNonNull(examType, "examType");
        this.subject = Objects.requireNonNull(subject, "subject");
        this.examWeight = Objects.requireNonNull(examWeight, "examWeight");
        this.sortOrder = sortOrder;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public ExamType getExamType() {
        return examType;
    }

    public String getSubject() {
        return subject;
    }

    public BigDecimal getExamWeight() {
        return examWeight;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
