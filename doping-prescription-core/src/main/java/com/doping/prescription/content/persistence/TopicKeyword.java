package com.doping.prescription.content.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** A normalized keyword of the rule-based tagging dictionary (docs/algoritma.md §5). */
@Entity
public class TopicKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer topicId;

    private String keyword;

    private BigDecimal weight;

    @Column(name = "is_negative")
    private boolean negative;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    protected TopicKeyword() {
        // for JPA
    }

    public TopicKeyword(Integer topicId, String keyword, BigDecimal weight, boolean negative) {
        this.topicId = Objects.requireNonNull(topicId, "topicId");
        this.keyword = Objects.requireNonNull(keyword, "keyword");
        this.weight = Objects.requireNonNull(weight, "weight");
        this.negative = negative;
    }

    public Integer getId() {
        return id;
    }

    public Integer getTopicId() {
        return topicId;
    }

    public String getKeyword() {
        return keyword;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public boolean isNegative() {
        return negative;
    }
}
