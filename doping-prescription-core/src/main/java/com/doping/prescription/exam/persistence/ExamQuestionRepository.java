package com.doping.prescription.exam.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Integer> {

    List<ExamQuestion> findByExamIdOrderByOrderNo(Integer examId);

    long countByExamId(Integer examId);
}
