package com.doping.prescription.content.domain;

import java.util.Objects;

/**
 * A topic suggested for a video by keyword tagging.
 *
 * @param topicId suggested topic
 * @param score   match score, 0-1; stored as both weight and confidence of the link
 */
public record TagSuggestion(Integer topicId, double score) {

    public TagSuggestion {
        Objects.requireNonNull(topicId, "topicId");
    }
}
