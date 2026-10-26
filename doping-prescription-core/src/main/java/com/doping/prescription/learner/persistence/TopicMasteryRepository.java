package com.doping.prescription.learner.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TopicMasteryRepository extends JpaRepository<TopicMastery, TopicMasteryId> {

    List<TopicMastery> findByIdUserId(UUID userId);

    /** The row locked until the transaction ends, so concurrent answers of the learner apply one after another. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from TopicMastery m where m.id = :id")
    Optional<TopicMastery> findForUpdate(@Param("id") TopicMasteryId id);
}
