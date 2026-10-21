package com.doping.prescription.feed.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedImpressionRepository extends JpaRepository<FeedImpression, UUID> {

    Optional<FeedImpression> findFirstByUserIdOrderByServedAtDesc(UUID userId);

    List<FeedImpression> findBySessionIdOrderByPositionAsc(UUID sessionId);
}
