package com.doping.prescription.feed.domain;

import com.doping.prescription.feed.domain.FeedContext.Video;
import java.util.Objects;

/**
 * A video offered by a pool for a target topic.
 *
 * @param targetTopicId the topic the card addresses; for popular cards the video's primary topic
 */
public record Candidate(Video video, PoolType pool, Integer targetTopicId) {

    public Candidate {
        Objects.requireNonNull(video, "video");
        Objects.requireNonNull(pool, "pool");
        Objects.requireNonNull(targetTopicId, "targetTopicId");
    }
}
