package com.doping.prescription.feed.domain;

import java.util.List;

/**
 * The components behind a card's score, as shown on the feed debug screen (docs/algoritma.md §3).
 *
 * @param weakness       how weak the learner is in the target topic
 * @param examImportance the topic's weight in the exam
 * @param quality        the video's quality score
 * @param freshness      1.0 if not watched before, 0.2 otherwise
 * @param difficultyFit  how well the video's difficulty fits the learner's mastery
 * @param appliedRules   notes about diversity rules that skipped or deferred candidates
 */
public record ScoreBreakdown(
        double weakness,
        double examImportance,
        double quality,
        double freshness,
        double difficultyFit,
        List<String> appliedRules) {

    public ScoreBreakdown {
        appliedRules = List.copyOf(appliedRules);
    }
}
