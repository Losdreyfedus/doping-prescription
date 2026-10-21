package com.doping.prescription.feed.domain;

/**
 * The learner module's mastery band as the feed receives it. The thresholds live only in the learner module; the feed
 * never recomputes a band (docs/algoritma.md §9).
 */
public enum TopicBand {
    UNKNOWN,
    WEAK,
    MEDIUM,
    STRONG
}
