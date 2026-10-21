package com.doping.prescription.points.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PointsLedgerRepository extends JpaRepository<PointsLedgerEntry, Long> {}
