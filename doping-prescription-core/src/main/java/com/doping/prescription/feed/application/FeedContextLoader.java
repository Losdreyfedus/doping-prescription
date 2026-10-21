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

    /** The learner's state against the given catalogue. */
    FeedContext load(UserSummary user, UUID sessionId, List<VideoView> videos, Map<Integer, TopicView> catalog) {
        Instant now = clock.instant();
        Map<Integer, TopicState> states = new HashMap<>();
        for (TopicProfile topic : profiles.profile(user.userId()).topics()) {
            TopicView view = catalog.get(topic.topicId());
            states.put(
                    topic.topicId(),
                    new TopicState(
                            topic.topicId(),
                            view.name(),
                            view.examWeight(),
                            topic.effectiveMastery(),
                            topic.confidence(),
                            TopicBand.valueOf(topic.band()),
                            topic.lastSignalAt(),
                            topic.lastExam().map(exam -> new ExamTally(exam.total(), exam.correct()))));
        }
        Set<Integer> deferred = queries.skipsPerTopic(sessionId).entrySet().stream()
                .filter(entry -> entry.getValue() >= properties.skipDeferCount())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        return new FeedContext(
                states,
                toVideos(videos),
                queries.completedSince(user.userId(), Instant.EPOCH),
                queries.completedSince(user.userId(), now.minus(Duration.ofDays(properties.rewatchBlockDays()))),
                seen(sessionId),
                deferred,
                now,
                examNear(user));
    }

    /** A context for the fallback feed only: the catalogue and the session, no learner data. */
    FeedContext fallbackContext(UUID sessionId, List<VideoView> videos) {
        return new FeedContext(
                Map.of(), toVideos(videos), Set.of(), Set.of(), seen(sessionId), Set.of(), clock.instant(), false);
    }

    private static List<Video> toVideos(List<VideoView> videos) {
        return videos.stream()
                .map(video ->
                        new Video(video.contentId(), video.primaryTopic(), video.difficulty(), video.qualityScore()))
                .toList();
    }

    /** Videos shown in the session, from Redis or, if it is unavailable, from the impressions. */
    Set<Integer> seen(UUID sessionId) {
        return store.seen(sessionId).orElseGet(() -> queries.sessionContent(sessionId));
    }

    private boolean examNear(UserSummary user) {
        LocalDate today = LocalDate.now(clock);
        return user.examDate()
                .map(date -> !date.isBefore(today) && !date.isAfter(today.plusDays(properties.examNearDays())))
                .orElse(false);
    }
}
