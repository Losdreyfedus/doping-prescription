package com.doping.prescription.points.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointsStreakRepository extends JpaRepository<PointsStreak, UUID> {

    /** The streak locked until the transaction ends, so concurrent answers of the learner apply one after another. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PointsStreak s where s.userId = :userId")
    Optional<PointsStreak> findForUpdate(@Param("userId") UUID userId);
}
