package com.doping.prescription.exam.persistence;

import com.doping.prescription.exam.domain.AttemptStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {

    Optional<ExamAttempt> findFirstByUserIdAndExamIdAndStatus(UUID userId, Integer examId, AttemptStatus status);

    Optional<ExamAttempt> findFirstByUserIdAndExamIdAndStatusOrderBySubmittedAtDesc(
            UUID userId, Integer examId, AttemptStatus status);
}
