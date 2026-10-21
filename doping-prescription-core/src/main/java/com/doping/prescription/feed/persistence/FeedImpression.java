package com.doping.prescription.feed.persistence;

import com.doping.prescription.feed.domain.PoolType;
import com.doping.prescription.feed.domain.ScoreBreakdown;
import com.doping.prescription.feed.domain.ServedCard;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A card shown to a learner. Immutable once served; interactions refer to it by id. */
@Entity
public class FeedImpression {

    @Id
    private UUID id;

    private UUID userId;

    private UUID sessionId;

    private Integer contentId;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int position;

    @Enumerated(EnumType.STRING)
    private PoolType pool;

    private Integer targetTopicId;

    private BigDecimal score;

    @JdbcTypeCode(SqlTypes.JSON)
    private ScoreBreakdown scoreBreakdown;

    private String reasonText;

    private String algoVersion;

    private Instant servedAt;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    protected FeedImpression() {
        // for JPA
    }

    public FeedImpression(UUID id, UUID userId, UUID sessionId, ServedCard card, String algoVersion, Instant servedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(card, "card");
        this.contentId = card.contentId();
        this.position = card.position();
        this.pool = card.pool();
        this.targetTopicId = card.targetTopicId();
        this.score = card.score();
        this.scoreBreakdown = card.breakdown();
        this.reasonText = card.reasonText();
        this.algoVersion = Objects.requireNonNull(algoVersion, "algoVersion");
        this.servedAt = Objects.requireNonNull(servedAt, "servedAt");
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public ServedCard getCard() {
        return new ServedCard(contentId, position, pool, targetTopicId, score, scoreBreakdown, reasonText);
    }

    public String getAlgoVersion() {
        return algoVersion;
    }

    public Instant getServedAt() {
        return servedAt;
    }
}
