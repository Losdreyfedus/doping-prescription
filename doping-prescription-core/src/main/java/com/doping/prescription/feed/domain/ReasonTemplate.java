package com.doping.prescription.feed.domain;

/** The "neden bu video" text for one pool (docs/algoritma.md §6, docs/mimari.md §10). Templates, never generated. */
public interface ReasonTemplate {

    PoolType pool();

    String reason(Candidate candidate, FeedContext context);
}
