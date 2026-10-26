package com.doping.prescription.feed.application;

import com.doping.prescription.auth.api.UserDirectory.UserSummary;
import com.doping.prescription.content.api.ContentCatalog.VideoView;
import com.doping.prescription.content.api.TopicCatalog.TopicView;
import com.doping.prescription.feed.domain.FeedContext;
import com.doping.prescription.feed.domain.FeedContext.ExamTally;
import com.doping.prescription.feed.domain.FeedContext.TopicState;
import com.doping.prescription.feed.domain.FeedContext.Video;
import com.doping.prescription.feed.domain.TopicBand;
import com.doping.prescription.feed.persistence.FeedQueries;
import com.doping.prescription.learner.api.LearnerProfileQuery;
import com.doping.prescription.learner.api.LearnerProfileQuery.Band;
import com.doping.prescription.learner.api.LearnerProfileQuery.LastExam;
import com.doping.prescription.learner.api.LearnerProfileQuery.TopicProfile;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Gathers a learner's {@link FeedContext} from the learner, content and feed data (one query per source). */
@Component
class FeedContextLoader {

    private final LearnerProfileQuery profiles;
    private final FeedQueries queries;
    private final FeedStore store;
    private final FeedProperties properties;
    private final Clock clock;

    FeedContextLoader(
            LearnerProfileQuery profiles,
            FeedQueries queries,
            FeedStore store,
            FeedProperties properties,
            Clock clock) {
        this.profiles = profiles;
        this.queries = queries;
        this.store = store;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * The learner's state against the given catalogue. Session reads are scoped to the learner: the session id comes
     * from the client's unsigned cursor, and another learner's session id must not leak their session.
     */
    FeedContext load(UserSummary user, UUID sessionId, List<VideoView> videos, Map<Integer, TopicView> catalog) {
        Instant now = clock.instant();
        UUID userId = user.userId();
        Map<Integer, TopicState> states = new HashMap<>();
        for (TopicProfile topic : profiles.profile(userId).topics()) {
            states.put(topic.topicId(), topicState(topic, catalog.get(topic.topicId())));
        }
        Set<Integer> deferred = queries.skipsPerTopic(userId, sessionId).entrySet().stream()
                .filter(entry -> entry.getValue() >= properties.skipDeferCount())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        return new FeedContext(
                states,
                toVideos(videos),
                queries.completedSince(userId, Instant.EPOCH),
                queries.completedSince(userId, now.minus(Duration.ofDays(properties.rewatchBlockDays()))),
                seen(userId, sessionId),
                deferred,
                queries.recentTopics(userId, sessionId, properties.maxSameTopicInRow()),
                now,
                examNear(user));
    }

    /** A context for the fallback feed only: the catalogue and the session, no learner data. */
    FeedContext fallbackContext(UUID userId, UUID sessionId, List<VideoView> videos) {
        return new FeedContext(
                Map.of(),
                toVideos(videos),
                Set.of(),
                Set.of(),
                seen(userId, sessionId),
                Set.of(),
                List.of(),
                clock.instant(),
                false);
    }

    private static TopicState topicState(TopicProfile topic, TopicView view) {
        LastExam exam = topic.lastExam();
        return new TopicState(
                topic.topicId(),
                view.name(),
                view.examWeight(),
                topic.effectiveMastery(),
                topic.confidence(),
                band(topic.band()),
                topic.lastSignalAt(),
                exam == null ? null : new ExamTally(exam.total(), exam.correct()));
    }

    /** The learner module's band as the feed's own type; a new band does not compile until it is mapped here. */
    static TopicBand band(Band band) {
        return switch (band) {
            case UNKNOWN -> TopicBand.UNKNOWN;
            case WEAK -> TopicBand.WEAK;
            case MEDIUM -> TopicBand.MEDIUM;
            case STRONG -> TopicBand.STRONG;
        };
    }

    private static List<Video> toVideos(List<VideoView> videos) {
        return videos.stream()
                .map(video ->
                        new Video(video.contentId(), video.primaryTopic(), video.difficulty(), video.qualityScore()))
                .toList();
    }

    /** Videos shown in the learner's session, from Redis or, if it is unavailable, from the impressions. */
    Set<Integer> seen(UUID userId, UUID sessionId) {
        return store.seen(userId, sessionId).orElseGet(() -> queries.sessionContent(userId, sessionId));
    }

    private boolean examNear(UserSummary user) {
        LocalDate today = LocalDate.now(clock);
        return user.examDate()
                .map(date -> !date.isBefore(today) && !date.isAfter(today.plusDays(properties.examNearDays())))
                .orElse(false);
    }
}
