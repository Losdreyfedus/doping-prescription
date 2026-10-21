package com.doping.prescription.content.persistence;

import com.doping.prescription.content.domain.TagSource;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Link between a video and a topic. A {@link TagSource#KEYWORD_RULE} link must be approved before its video goes LIVE.
 */
@Entity
@Table(name = "content_topic_map")
public class ContentTopic {

    @EmbeddedId
    private ContentTopicId id;

    private BigDecimal weight;

    @Enumerated(EnumType.STRING)
    private TagSource tagSource;

    private BigDecimal confidence;

    private UUID approvedBy;

    private Instant approvedAt;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected ContentTopic() {
        // for JPA
    }

    public ContentTopic(ContentTopicId id, BigDecimal weight, TagSource tagSource, BigDecimal confidence, Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        this.weight = Objects.requireNonNull(weight, "weight");
        this.tagSource = Objects.requireNonNull(tagSource, "tagSource");
        this.confidence = Objects.requireNonNull(confidence, "confidence");
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public void approve(UUID adminId, Instant now) {
        this.approvedBy = Objects.requireNonNull(adminId, "adminId");
        this.approvedAt = now;
        this.updatedAt = now;
    }

    public ContentTopicId getId() {
        return id;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public TagSource getTagSource() {
        return tagSource;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public Optional<UUID> getApprovedBy() {
        return Optional.ofNullable(approvedBy);
    }

    public Optional<Instant> getApprovedAt() {
        return Optional.ofNullable(approvedAt);
    }
}
