package com.doping.prescription.feed.application;

import com.doping.prescription.feed.domain.CandidatePool;
import com.doping.prescription.feed.domain.FeedComposer;
import com.doping.prescription.feed.domain.FeedRanker;
import com.doping.prescription.feed.domain.Pools;
import com.doping.prescription.feed.domain.ReasonTemplate;
import com.doping.prescription.feed.domain.ReasonTemplates;
import com.doping.prescription.feed.domain.WeightedLinearRanker;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The feed's extension points as beans (docs/mimari.md §10): a new pool, ranker or reason template is a new bean here;
 * the composer receives all of them.
 */
@Configuration(proxyBeanMethods = false)
class FeedBeans {

    @Bean
    CandidatePool weakTopicPool(FeedProperties properties) {
        return Pools.weakTopic(properties.weakTopicCount());
    }

    @Bean
    CandidatePool reviewDuePool(FeedProperties properties) {
        return Pools.reviewDue(Duration.ofDays(properties.reviewAfterDays()));
    }

    @Bean
    CandidatePool explorePool() {
        return Pools.explore();
    }

    @Bean
    CandidatePool popularPool() {
        return Pools.popular();
    }

    @Bean
    FeedRanker weightedLinearRanker(FeedProperties properties) {
        return new WeightedLinearRanker(properties.rankWeights().toDomain(properties.examNearWeight()));
    }

    @Bean
    ReasonTemplate weakTopicReason() {
        return ReasonTemplates.weakTopic();
    }

    @Bean
    ReasonTemplate reviewDueReason() {
        return ReasonTemplates.reviewDue();
    }

    @Bean
    ReasonTemplate exploreReason() {
        return ReasonTemplates.explore();
    }

    @Bean
    ReasonTemplate popularReason() {
        return ReasonTemplates.popular();
    }

    @Bean
    ReasonTemplate fallbackReason() {
        return ReasonTemplates.fallback();
    }

    @Bean
    FeedComposer feedComposer(
            List<CandidatePool> pools, FeedRanker ranker, List<ReasonTemplate> templates, FeedProperties properties) {
        return new FeedComposer(pools, ranker, templates, properties.maxSameTopicInRow());
    }
}
