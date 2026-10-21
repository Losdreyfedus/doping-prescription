package com.doping.prescription.feed.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedImpressionRepository extends JpaRepository<FeedImpression, UUID> {}
