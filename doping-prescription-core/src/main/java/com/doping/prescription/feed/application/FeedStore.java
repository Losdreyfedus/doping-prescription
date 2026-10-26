package com.doping.prescription.feed.application;

import com.doping.prescription.feed.domain.ServedCard;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The prepared queue and the session's seen videos (docs/algoritma.md §4). Never the source of truth: an empty
 * {@link Optional} means the store is unavailable and the caller computes from the database instead (docs/mimari.md §5).
 */
public interface FeedStore {

    /** The prepared cards, possibly an empty list; empty Optional if the store is unavailable. */
    Optional<List<ServedCard>> queue(UUID userId);

    void replaceQueue(UUID userId, List<ServedCard> cards);

    void dropQueue(UUID userId);

    /**
     * Videos shown in the learner's session; empty Optional if the store is unavailable. Keyed by learner and session,
     * because the session id comes from the client's cursor.
     */
    Optional<Set<Integer>> seen(UUID userId, UUID sessionId);

    void markSeen(UUID userId, UUID sessionId, Collection<Integer> contentIds);
}
