package com.doping.prescription.content.persistence;

import jakarta.persistence.Embeddable;
import java.io.Serializable;

/**
 * Composite key of {@link ContentTopic}.
 *
 * @param contentId the video
 * @param topicId   the topic it covers
 */
@Embeddable
public record ContentTopicId(Integer contentId, Integer topicId) implements Serializable {}
