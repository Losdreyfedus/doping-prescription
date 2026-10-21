package com.doping.prescription.content.domain;

import java.util.List;
import java.util.Objects;

/**
 * A dictionary entry of rule-based tagging.
 *
 * @param topicId  topic the keyword points to
 * @param keyword  normalized keyword, possibly several words (e.g. {@code tanim kumesi})
 * @param weight   contribution to the topic score, 0-1
 * @param negative true if a match excludes the topic
 */
public record TaggingKeyword(Integer topicId, String keyword, double weight, boolean negative) {

    public TaggingKeyword {
        Objects.requireNonNull(topicId, "topicId");
        Objects.requireNonNull(keyword, "keyword");
        if (TextNormalizer.tokens(keyword).isEmpty()) {
            throw new IllegalArgumentException("keyword must contain at least one word");
        }
        if (weight < 0 || weight > 1) {
            throw new IllegalArgumentException("weight must be between 0 and 1");
        }
    }

    List<String> tokens() {
        return TextNormalizer.tokens(keyword);
    }
}
