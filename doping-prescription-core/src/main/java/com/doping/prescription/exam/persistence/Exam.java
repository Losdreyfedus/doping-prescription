package com.doping.prescription.exam.persistence;

import com.doping.prescription.exam.domain.ExamType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.Objects;

/** A ready-made mini exam. */
@Entity
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String title;

    @Enumerated(EnumType.STRING)
    private ExamType examType;

    private int durationMin;

    @Column(name = "is_active")
    private boolean active;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected Exam() {
        // for JPA
    }

    public Exam(String title, ExamType examType, int durationMin, Instant now) {
        this.title = Objects.requireNonNull(title, "title");
        this.examType = Objects.requireNonNull(examType, "examType");
        this.durationMin = durationMin;
        this.active = true;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public ExamType getExamType() {
        return examType;
    }

    public int getDurationMin() {
        return durationMin;
    }

    public boolean isActive() {
        return active;
    }
}
