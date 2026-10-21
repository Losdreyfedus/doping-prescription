package com.doping.prescription.learner.persistence;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

/**
 * Composite key of {@link TopicMastery}.
 *
 * @param userId  the learner
 * @param topicId the topic
 */
@Embeddable
public record TopicMasteryId(UUID userId, Integer topicId) implements Serializable {}
