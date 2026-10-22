package com.doping.prescription.quiz.persistence;

import com.doping.prescription.quiz.api.QuizStatistics;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MicroQuizAttemptRepository extends JpaRepository<MicroQuizAttempt, UUID> {

    @Query("""
            select new com.doping.prescription.quiz.api.QuizStatistics$QuestionStats(
                a.microQuestionId, count(a), sum(case when a.correct = true then 1L else 0L end))
            from MicroQuizAttempt a
            group by a.microQuestionId
            """)
    List<QuizStatistics.QuestionStats> countByQuestion();

    @Query("select a.microQuestionId from MicroQuizAttempt a where a.userId = :userId")
    List<Integer> findAnsweredQuestionIds(@Param("userId") UUID userId);

    @Query("select count(a) from MicroQuizAttempt a where a.userId = :userId and a.createdAt > :since")
    long countSince(@Param("userId") UUID userId, @Param("since") Instant since);
}
