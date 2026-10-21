package com.doping.prescription.feed.domain;

import java.util.List;

/**
 * One source of feed candidates (docs/algoritma.md §2, docs/mimari.md §10). A new pool is a new implementation; the
 * composer and the quota table pick it up by its {@link PoolType}.
 */
public interface CandidatePool {

    PoolType type();

    /** Candidates in the pool's own preference order; the composer ranks and filters them. */
    List<Candidate> candidates(FeedContext context);
}
