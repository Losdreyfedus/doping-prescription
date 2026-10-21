package com.doping.prescription.content.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Descriptive data of a video as an editor enters it.
 *
 * @param title        title shown on the feed card
 * @param description  text used by keyword tagging
 * @param teacherName  fictional teacher name
 * @param videoUrl     real video file, or {@code null} for the simulated player
 * @param durationSec  real video length in seconds
 * @param difficulty   1 (easy) to 5 (hard)
 * @param kind         video format
 * @param qualityScore 0-1, static in v1
 */
public record VideoDetails(
        String title,
        String description,
        String teacherName,
        String videoUrl,
        int durationSec,
        int difficulty,
        ContentKind kind,
        BigDecimal qualityScore) {

    public VideoDetails {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(teacherName, "teacherName");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(qualityScore, "qualityScore");
    }
}
