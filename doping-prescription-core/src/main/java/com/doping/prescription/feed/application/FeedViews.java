package com.doping.prescription.feed.application;

import com.doping.prescription.content.api.ContentCatalog.VideoView;
import com.doping.prescription.content.api.TopicCatalog.TopicView;
import com.doping.prescription.feed.application.ImpressionWriter.Served;
import com.doping.prescription.feed.domain.PoolType;
import com.doping.prescription.points.api.PointsLedger;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** The feed's response bodies (docs/api.md, GET /api/feed) and their assembly. */
@Component
public class FeedViews {

    private final PointsLedger points;

    FeedViews(PointsLedger points) {
        this.points = points;
    }

    /** @param videos LIVE videos by id; the card's video must be among them */
    CardView card(Served served, Map<Integer, VideoView> videos, Map<Integer, TopicView> catalog, boolean saved) {
        VideoView video = Objects.requireNonNull(videos.get(served.card().contentId()), "served video is not LIVE");
        TopicView topic = catalog.get(served.card().targetTopicId());
        return new CardView(
                served.impressionId(),
                served.card().position(),
                new ContentView(
                        video.contentId(),
                        video.title(),
                        video.teacherName(),
                        video.videoUrl(),
                        video.durationSec(),
                        video.contentKind()),
                new TopicRef(topic.topicId(), topic.code(), topic.name()),
                served.card().pool().name(),
                served.card().reasonText(),
                video.hasMicroQuiz(),
                points.pointsAvailable(served.card().pool() == PoolType.WEAK_TOPIC),
                saved);
    }

    /**
     * @param personalized   false for the fallback feed: personalization off, no signals yet, or an error
     * @param nextCursor     pass back to get the next page of the same session
     * @param demoVideoSpeed how much faster the simulated player runs (docs/arayuz.md)
     */
    public record FeedPage(
            UUID sessionId,
            boolean personalized,
            String algoVersion,
            String nextCursor,
            int demoVideoSpeed,
            List<CardView> cards) {

        public FeedPage {
            cards = List.copyOf(cards);
        }
    }

    /**
     * @param pool            WEAK_TOPIC, REVIEW_DUE, EXPLORE, POPULAR or FALLBACK
     * @param reasonText      the "neden bu video" text
     * @param pointsAvailable points a correct micro-question answer earns
     * @param saved           whether the learner saved the video (the card's bookmark)
     */
    public record CardView(
            UUID impressionId,
            int position,
            ContentView content,
            TopicRef topic,

            @Schema(allowableValues = {"WEAK_TOPIC", "REVIEW_DUE", "EXPLORE", "POPULAR", "FALLBACK"})
            String pool,

            String reasonText,
            boolean hasMicroQuiz,
            int pointsAvailable,
            boolean saved) {}

    /**
     * @param videoUrl {@code null}: the web app uses the simulated player
     */
    public record ContentView(
            Integer contentId,
            String title,
            String teacherName,
            @Schema(nullable = true) @Nullable String videoUrl,
            int durationSec,

            @Schema(allowableValues = {"CONCEPT", "SOLVED_EXAMPLE", "TIP"})
            String contentKind) {}

    public record TopicRef(Integer topicId, String code, String name) {}
}
