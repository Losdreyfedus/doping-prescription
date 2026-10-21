package com.doping.prescription.feed.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Everything the feed algorithm needs about one learner at one moment, gathered by the application layer so the
 * algorithm itself stays pure (AGENTS.md rule 4).
 *
 * @param topics             every topic's state for the learner, by topic id
 * @param videos             LIVE videos
 * @param watchedEver        videos the learner completed at any time (freshness)
 * @param completedRecently  videos completed within the rewatch block window (rule 2)
 * @param excluded           videos already shown in this session, never offered again (session integrity)
 * @param deferredTopics     topics skipped too often in this session (rule 3)
 * @param now                the current instant
 * @param examNear           whether the learner's exam date is close (examImportance weight changes)
 */
public record FeedContext(
        Map<Integer, TopicState> topics,
        List<Video> videos,
        Set<Integer> watchedEver,
        Set<Integer> completedRecently,
        Set<Integer> excluded,
        Set<Integer> deferredTopics,
        Instant now,
        boolean examNear) {

    public FeedContext {
        topics = Map.copyOf(topics);
        videos = List.copyOf(videos);
        watchedEver = Set.copyOf(watchedEver);
        completedRecently = Set.copyOf(completedRecently);
        excluded = Set.copyOf(excluded);
        deferredTopics = Set.copyOf(deferredTopics);
        Objects.requireNonNull(now, "now");
    }

    public TopicState topic(Integer topicId) {
        return Objects.requireNonNull(topics.get(topicId), () -> "unknown topic " + topicId);
    }

    /** True if the learner has any mastery signal; otherwise the feed is a cold start (docs/algoritma.md §2). */
    public boolean hasSignals() {
        return topics.values().stream().anyMatch(topic -> topic.band() != TopicBand.UNKNOWN);
    }

    /**
     * A topic as the feed sees it: the learner's decayed mastery and band from the learner module, plus catalogue
     * data.
     *
     * @param lastSignalAt when the learner last produced a mastery signal on the topic
     * @param lastExam     the topic's result in the learner's latest exam
     */
    public record TopicState(
            Integer topicId,
            String name,
            double examWeight,
            double mastery,
            double confidence,
            TopicBand band,
            Optional<Instant> lastSignalAt,
            Optional<ExamTally> lastExam) {}

    /** "3 questions, 0 correct". */
    public record ExamTally(int total, int correct) {}

    /**
     * A LIVE video.
     *
     * @param topicId    the video's primary topic
     * @param difficulty 1 (easy) to 5 (hard)
     * @param quality    editorial quality score, 0-1
     */
    public record Video(Integer contentId, Integer topicId, int difficulty, double quality) {}
}
