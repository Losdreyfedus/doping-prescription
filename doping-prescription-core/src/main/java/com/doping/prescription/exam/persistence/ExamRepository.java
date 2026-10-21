package com.doping.prescription.exam.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<Exam, Integer> {

    List<Exam> findByActiveTrueOrderById();
}
