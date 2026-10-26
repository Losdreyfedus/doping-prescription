package com.doping.prescription.learner.persistence;

import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LearnerSignalRepository extends JpaRepository<LearnerSignal, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    /** Which of the keys are already recorded, in one query. */
    @Query("select s.idempotencyKey from LearnerSignal s where s.idempotencyKey in :keys")
    Set<String> findRecordedKeys(@Param("keys") Collection<String> keys);
}
