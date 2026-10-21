package com.doping.prescription.points.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointsLedgerRepository extends JpaRepository<PointsLedgerEntry, Long> {

    boolean existsByRefId(String refId);

    @Query(
            "select coalesce(sum(e.amount), 0) from PointsLedgerEntry e where e.userId = :userId and e.createdAt >= :since")
    int sumSince(@Param("userId") UUID userId, @Param("since") Instant since);

    List<PointsLedgerEntry> findTop10ByUserIdOrderByCreatedAtDescIdDesc(UUID userId);
}
