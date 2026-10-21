package com.doping.prescription.exam.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, ExamAnswerId> {

    List<ExamAnswer> findByIdAttemptId(UUID attemptId);
}
