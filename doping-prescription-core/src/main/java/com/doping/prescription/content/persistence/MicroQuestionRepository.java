package com.doping.prescription.content.persistence;

import com.doping.prescription.content.domain.MicroQuestionStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MicroQuestionRepository extends JpaRepository<MicroQuestion, Integer> {

    List<MicroQuestion> findByContentIdAndStatusOrderByIdAsc(Integer contentId, MicroQuestionStatus status);
}
