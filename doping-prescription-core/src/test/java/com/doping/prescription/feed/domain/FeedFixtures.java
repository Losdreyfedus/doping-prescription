package com.doping.prescription.feed.domain;

import com.doping.prescription.feed.domain.FeedContext.ExamTally;
import com.doping.prescription.feed.domain.FeedContext.TopicState;
import com.doping.prescription.feed.domain.FeedContext.Video;
import com.doping.prescription.feed.domain.WeightedLinearRanker.RankWeights;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** A catalogue and learner shaped like the demo seed: 10 topics with 4 videos each. */
final class FeedFixtures {

    static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");
    static final int PERKOM = 8;
    static final int OLASILIK = 9;
    static final int FONKSIYONLAR = 10;
    static final RankWeights WEIGHTS = new RankWeights(0.45, 0.20, 0.15, 0.10, 0.10, 0.30);

    private FeedFixtures() {}

    static Map<PoolType, Double> quotas() {
        Map<PoolType, Double> quotas = new EnumMap<>(PoolType.class);
        quotas.put(PoolType.WEAK_TOPIC, 0.60);
        quotas.put(PoolType.REVIEW_DUE, 0.15);
        quotas.put(PoolType.EXPLORE, 0.15);
        quotas.put(PoolType.POPULAR, 0.10);
        return quotas;
    }

    static FeedComposer composer() {
        return new FeedComposer(
                List.of(Pools.weakTopic(5), Pools.reviewDue(Duration.ofDays(7)), Pools.explore(), Pools.popular()),
                new WeightedLinearRanker(WEIGHTS),
                ReasonTemplates.all(),
                2);
    }

    static List<Video> videos() {
        List<Video> videos = new ArrayList<>();
        for (int topic = 1; topic <= 10; topic++) {
            for (int index = 0; index < 4; index++) {
                videos.add(new Video(topic * 100 + index, topic, 1 + index, 0.6 + 0.1 * (index % 3)));
            }
        }
        return videos;
    }

    /** Deniz after the scenario exam: Olasılık 3 wrong, PerKom 1 correct + 1 blank, 7 topics correct, rest unknown. */
    static Map<Integer, TopicState> afterScenarioExam() {
        Map<Integer, TopicState> topics = new HashMap<>();
        for (int topic = 1; topic <= 10; topic++) {
            topics.put(topic, unknown(topic));
        }
        for (int topic = 1; topic <= 7; topic++) {
            topics.put(topic, state(topic, 0.65, 0.181, TopicBand.MEDIUM, new ExamTally(1, 1)));
        }
        topics.put(PERKOM, state(PERKOM, 0.494, 0.330, TopicBand.WEAK, new ExamTally(2, 1)));
        topics.put(OLASILIK, state(OLASILIK, 0.1715, 0.4512, TopicBand.WEAK, new ExamTally(3, 0)));
        return topics;
    }

    static Map<Integer, TopicState> noSignals() {
        Map<Integer, TopicState> topics = new HashMap<>();
        for (int topic = 1; topic <= 10; topic++) {
            topics.put(topic, unknown(topic));
        }
        return topics;
    }

    static FeedContext context(Map<Integer, TopicState> topics) {
        return new FeedContext(topics, videos(), Set.of(), Set.of(), Set.of(), Set.of(), NOW, false);
    }

    static TopicState state(int topic, double mastery, double confidence, TopicBand band, ExamTally exam) {
        return new TopicState(
                topic, name(topic), examWeight(topic), mastery, confidence, band, Optional.of(NOW), Optional.of(exam));
    }

    private static TopicState unknown(int topic) {
        return new TopicState(
                topic, name(topic), examWeight(topic), 0.5, 0, TopicBand.UNKNOWN, Optional.empty(), Optional.empty());
    }

    static String name(int topic) {
        return switch (topic) {
            case PERKOM -> "Permütasyon-Kombinasyon";
            case OLASILIK -> "Olasılık";
            case FONKSIYONLAR -> "Fonksiyonlar";
            default -> "Konu " + topic;
        };
    }

    private static double examWeight(int topic) {
        return topic == FONKSIYONLAR ? 0.6 : 0.5;
    }
}
