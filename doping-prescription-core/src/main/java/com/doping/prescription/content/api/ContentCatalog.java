package com.doping.prescription.content.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Read access to the video catalogue for other modules (docs/mimari.md §1). */
public interface ContentCatalog {

    /**
     * Number of LIVE videos per topic, for "Bu konudan N video hazır". A video counts for its primary topic: the
     * topic with the highest weight in content_topic_map (docs/algoritma.md §2). Topics without videos are absent.
     */
    Map<Integer, Integer> liveVideoCountByTopic();

    /** Every LIVE video with its primary topic, the feed's candidate catalogue. */
    List<VideoView> liveVideos();

    /** Videos by id regardless of status, in one query. */
    List<VideoView> videos(Collection<Integer> contentIds);

    /** A video by id regardless of status, for cards already served. */
    Optional<VideoView> video(Integer contentId);

    /**
     * @param videoUrl      file under the web app's public folder; {@code null} means the simulated player
     * @param contentKind   CONCEPT, SOLVED_EXAMPLE or TIP
     * @param difficulty    1 (easy) to 5 (hard)
     * @param qualityScore  editorial quality, 0-1
     * @param primaryTopic  the topic with the highest weight
     * @param hasMicroQuiz  whether a LIVE micro-question is attached
     */
    record VideoView(
            Integer contentId,
            String title,
            String teacherName,
            @Nullable String videoUrl,
            int durationSec,
            String contentKind,
            int difficulty,
            double qualityScore,
            Integer primaryTopic,
            boolean hasMicroQuiz) {}
}
