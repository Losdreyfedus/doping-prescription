package com.doping.prescription.feed.domain;

/** Scores a candidate (docs/algoritma.md §3, docs/mimari.md §10). */
public interface FeedRanker {

    /** Short identifier stored with every impression, e.g. {@code v1}. */
    String version();

    Ranked rank(Candidate candidate, FeedContext context);

    /**
     * @param score     the final score
     * @param breakdown its components, kept for the debug API
     */
    record Ranked(Candidate candidate, double score, ScoreBreakdown breakdown) {}
}
