package com.doping.prescription.feed.domain;

import com.doping.prescription.feed.domain.FeedContext.TopicState;
import com.doping.prescription.feed.domain.FeedContext.Video;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** The candidate pools of docs/algoritma.md §2. */
public final class Pools {

    private Pools() {}

    /** Videos of the weakest WEAK topics, weakest first. */
    public static CandidatePool weakTopic(int weakTopicCount) {
        return new TopicPool(
                PoolType.WEAK_TOPIC,
                (topic, context) -> topic.band() == TopicBand.WEAK,
                Comparator.comparingDouble(TopicState::mastery),
                weakTopicCount);
    }

    /** Videos of MEDIUM or STRONG topics not practised for {@code reviewAfter}. */
    public static CandidatePool reviewDue(Duration reviewAfter) {
        return new TopicPool(
                PoolType.REVIEW_DUE,
                (topic, context) -> (topic.band() == TopicBand.MEDIUM || topic.band() == TopicBand.STRONG)
                        && Optional.ofNullable(topic.lastSignalAt())
                                .map(last -> !last.plus(reviewAfter).isAfter(context.now()))
                                .orElse(false),
                Comparator.comparingDouble(TopicState::mastery),
                Integer.MAX_VALUE);
    }

    /** Videos of topics without any signal, most important for the exam first. */
    public static CandidatePool explore() {
        return new TopicPool(
                PoolType.EXPLORE,
                (topic, context) -> topic.band() == TopicBand.UNKNOWN,
                Comparator.comparingDouble(TopicState::examWeight).reversed(),
                Integer.MAX_VALUE);
    }

    /** Every LIVE video, best quality first. */
    public static CandidatePool popular() {
        return new CandidatePool() {
            @Override
            public PoolType type() {
                return PoolType.POPULAR;
            }

            @Override
            public List<Candidate> candidates(FeedContext context) {
                return context.videos().stream()
                        .sorted(Comparator.comparingDouble(Video::quality).reversed())
                        .map(video -> new Candidate(video, PoolType.POPULAR, video.topicId()))
                        .toList();
            }
        };
    }

    private interface TopicFilter {
        boolean test(TopicState topic, FeedContext context);
    }

    /** A pool that picks topics, then offers their videos. */
    private record TopicPool(PoolType type, TopicFilter filter, Comparator<TopicState> order, int maxTopics)
            implements CandidatePool {

        @Override
        public List<Candidate> candidates(FeedContext context) {
            Map<Integer, List<Video>> videosByTopic =
                    context.videos().stream().collect(Collectors.groupingBy(Video::topicId));
            Predicate<TopicState> selected = topic -> filter.test(topic, context);
            return context.topics().values().stream()
                    .filter(selected)
                    .sorted(order.thenComparing(TopicState::topicId))
                    .limit(maxTopics)
                    .flatMap(topic -> videosByTopic.getOrDefault(topic.topicId(), List.of()).stream())
                    .map(video -> new Candidate(video, type, video.topicId()))
                    .toList();
        }
    }
}
