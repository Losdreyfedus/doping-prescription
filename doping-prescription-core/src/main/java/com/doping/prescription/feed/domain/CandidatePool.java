package com.doping.prescription.feed.domain;

/** Source of a feed card (docs/algoritma.md §2). */
public enum CandidatePool {
    WEAK_TOPIC,
    REVIEW_DUE,
    EXPLORE,
    POPULAR,
    /** Personalization off, no signals yet, or ranking failed. */
    FALLBACK
}
