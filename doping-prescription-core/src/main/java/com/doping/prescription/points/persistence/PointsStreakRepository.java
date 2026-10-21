package com.doping.prescription.points.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointsStreakRepository extends JpaRepository<PointsStreak, UUID> {}
