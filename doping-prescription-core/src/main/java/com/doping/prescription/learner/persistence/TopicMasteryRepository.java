package com.doping.prescription.learner.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicMasteryRepository extends JpaRepository<TopicMastery, TopicMasteryId> {

    List<TopicMastery> findByIdUserId(UUID userId);
}
