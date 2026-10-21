package com.doping.prescription.content.persistence;

import com.doping.prescription.content.domain.ContentKind;
import com.doping.prescription.content.domain.ContentStatus;
import com.doping.prescription.content.domain.VideoDetails;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A short Dopingle video. A new video starts as {@link ContentStatus#DRAFT}. */
@Entity
@Table(name = "content_item")
public class ContentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String title;

    private String description;

    private String teacherName;

    private String videoUrl;

    private int durationSec;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int difficulty;

    @Enumerated(EnumType.STRING)
    private ContentKind contentKind;

    private BigDecimal qualityScore;

    @Enumerated(EnumType.STRING)
    private ContentStatus status;

    private Instant publishedAt;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected ContentItem() {
        // for JPA
    }

    public ContentItem(VideoDetails details, Instant now) {
        Objects.requireNonNull(details, "details");
        this.title = details.title();
        this.description = details.description();
        this.teacherName = details.teacherName();
        this.videoUrl = details.videoUrl();
        this.durationSec = details.durationSec();
        this.difficulty = details.difficulty();
        this.contentKind = details.kind();
        this.qualityScore = details.qualityScore();
        this.status = ContentStatus.DRAFT;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    /** Keyword tagging found topics for this draft; an editor still has to approve them before it goes LIVE. */
    public void markTagged(Instant now) {
        if (status != ContentStatus.DRAFT) {
            throw new IllegalStateException("Only a DRAFT video can be tagged, video " + id + " is " + status);
        }
        this.status = ContentStatus.TAGGED;
        this.updatedAt = now;
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public Optional<String> getVideoUrl() {
        return Optional.ofNullable(videoUrl);
    }

    public int getDurationSec() {
        return durationSec;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public ContentKind getContentKind() {
        return contentKind;
    }

    public BigDecimal getQualityScore() {
        return qualityScore;
    }

    public ContentStatus getStatus() {
        return status;
    }

    public Optional<Instant> getPublishedAt() {
        return Optional.ofNullable(publishedAt);
    }
}
