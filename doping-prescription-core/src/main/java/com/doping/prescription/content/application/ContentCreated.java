package com.doping.prescription.content.application;

import java.util.Objects;

/**
 * A video was created. Module-internal application event: it starts keyword tagging and never leaves the content module
 * (docs/mimari.md §3).
 *
 * @param contentId the new video
 */
public record ContentCreated(Integer contentId) {

    public ContentCreated {
        Objects.requireNonNull(contentId, "contentId");
    }
}
