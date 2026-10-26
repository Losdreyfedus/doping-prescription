package com.doping.prescription.feed.application;

import com.doping.prescription.feed.domain.PoolType;
import com.doping.prescription.feed.domain.WeightedLinearRanker.RankWeights;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration of the feed module, {@code prescription.feed.*} (docs/algoritma.md §9).
 *
 * @param pageSize             cards per page when the client does not ask for another size
 * @param pageSizeMax          largest page a client may ask for
 * @param queueSize            cards prepared in the Redis queue
 * @param queueTtl             lifetime of a prepared queue
 * @param queueRefillThreshold the queue is rebuilt when fewer cards are left
 * @param sessionTtl           lifetime of a session's seen-video set
 * @param storeRetryAfter      after a Redis failure the feed skips Redis for this long (docs/mimari.md §5)
 * @param quota                pool shares of a personalized page
 * @param coldStartQuota       pool shares for a learner without signals
 * @param weakTopicCount       weakest topics feeding WEAK_TOPIC
 * @param reviewAfterDays      days without practice after which a topic is due for review
 * @param rankWeights          weights of the linear score
 * @param examNearDays         exam date closer than this many days raises examImportance's weight
 * @param examNearWeight       examImportance's weight then
 * @param maxSameTopicInRow    rule 1
 * @param rewatchBlockDays     rule 2
 * @param skipDeferCount       rule 3
 * @param demoVideoSpeed       how much faster the simulated player runs, passed to the web app
 * @param pinnedContentIds     videos that open a personalized session, in order (the demo's real videos); empty by default
 */
@Validated
@ConfigurationProperties(prefix = "prescription.feed")
public record FeedProperties(
        @Positive int pageSize,
        @Positive int pageSizeMax,
        @Positive int queueSize,
        @NotNull Duration queueTtl,
        @Positive int queueRefillThreshold,
        @NotNull Duration sessionTtl,
        @NotNull Duration storeRetryAfter,
        @NotNull @Valid Quota quota,
        @NotNull @Valid ColdStartQuota coldStartQuota,
        @Positive int weakTopicCount,
        @Positive int reviewAfterDays,
        @NotNull @Valid Weights rankWeights,
        @Positive int examNearDays,
        @DecimalMin("0") double examNearWeight,
        @Positive int maxSameTopicInRow,
        @Positive int rewatchBlockDays,
        @Positive int skipDeferCount,
        @Positive int demoVideoSpeed,
        @NotNull List<Integer> pinnedContentIds) {

    public FeedProperties {
        pinnedContentIds = pinnedContentIds == null ? List.of() : List.copyOf(pinnedContentIds);
    }

    private static final double SUM_TOLERANCE = 1e-9;

    @AssertTrue(message = "page-size must not exceed page-size-max or queue-size")
    boolean isPageSizeConsistent() {
        return pageSize <= pageSizeMax && pageSize <= queueSize;
    }

    /** Pages kept in a queue. */
    public int queuePages() {
        return queueSize / pageSize;
    }

    /** Shares of a personalized page; they must add up to 1. */
    public record Quota(
            @DecimalMin("0") @DecimalMax("1") double weakTopic,
            @DecimalMin("0") @DecimalMax("1") double reviewDue,
            @DecimalMin("0") @DecimalMax("1") double explore,
            @DecimalMin("0") @DecimalMax("1") double popular) {

        @AssertTrue(message = "quota shares must add up to 1")
        boolean isComplete() {
            return Math.abs(weakTopic + reviewDue + explore + popular - 1) < SUM_TOLERANCE;
        }

        public Map<PoolType, Double> shares() {
            Map<PoolType, Double> shares = new EnumMap<>(PoolType.class);
            shares.put(PoolType.WEAK_TOPIC, weakTopic);
            shares.put(PoolType.REVIEW_DUE, reviewDue);
            shares.put(PoolType.EXPLORE, explore);
            shares.put(PoolType.POPULAR, popular);
            return shares;
        }
    }

    /** Shares for a learner without any signal; they must add up to 1. */
    public record ColdStartQuota(
            @DecimalMin("0") @DecimalMax("1") double explore,
            @DecimalMin("0") @DecimalMax("1") double popular) {

        @AssertTrue(message = "cold-start quota shares must add up to 1")
        boolean isComplete() {
            return Math.abs(explore + popular - 1) < SUM_TOLERANCE;
        }

        public Map<PoolType, Double> shares() {
            Map<PoolType, Double> shares = new EnumMap<>(PoolType.class);
            shares.put(PoolType.EXPLORE, explore);
            shares.put(PoolType.POPULAR, popular);
            return shares;
        }
    }

    public record Weights(
            @DecimalMin("0") double weakness,
            @DecimalMin("0") double examImportance,
            @DecimalMin("0") double quality,
            @DecimalMin("0") double freshness,
            @DecimalMin("0") double difficultyFit) {

        public RankWeights toDomain(double examNearWeight) {
            return new RankWeights(weakness, examImportance, quality, freshness, difficultyFit, examNearWeight);
        }
    }
}
