package com.doping.prescription.quiz.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MicroQuizAttemptRepository extends JpaRepository<MicroQuizAttempt, UUID> {}
