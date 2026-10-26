package com.doping.prescription.exam.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Integer> {

    List<ExamQuestion> findByExamIdOrderByOrderNo(Integer examId);

    @Query(
            "select new com.doping.prescription.exam.persistence.QuestionCount(q.examId, count(q)) from ExamQuestion q group by q.examId")
    List<QuestionCount> countPerExam();
}
