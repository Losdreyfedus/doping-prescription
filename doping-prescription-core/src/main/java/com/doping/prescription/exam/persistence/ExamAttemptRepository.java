package com.doping.prescription.exam.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {}
