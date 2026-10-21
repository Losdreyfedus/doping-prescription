package com.doping.prescription.learner.application;

import java.util.Optional;
import java.util.UUID;

/**
 * Cache of {@link LearnerSnapshot}s. Never the source of truth: every failure behaves like a miss. Readers fill a miss
 * only if the entry is still absent, and writers overwrite it after their commit, so a reader that loaded old values
 * before a write can never cache them over the new ones.
 */
public interface SnapshotCache {

    Optional<LearnerSnapshot> get(UUID userId);

    /** Read path: caches the snapshot unless another one got there first. */
    void putIfAbsent(UUID userId, LearnerSnapshot snapshot);

    /** Drops the cached snapshot, e.g. after the learner's data was erased. */
    void evict(UUID userId);

    /** Write path: replaces whatever is cached with the snapshot loaded after the commit. */
    void replace(UUID userId, LearnerSnapshot snapshot);
}
