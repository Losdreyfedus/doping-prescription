package com.doping.prescription.content.domain;

/** Lifecycle of a video; only {@link #LIVE} videos enter the feed (docs/algoritma.md §5). */
public enum ContentStatus {
    DRAFT,
    TAGGED,
    LIVE,
    RETIRED
}
