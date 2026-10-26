package com.doping.prescription.exam.persistence;

import com.doping.prescription.exam.domain.AttemptStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {

    List<ExamAttempt> findByUserId(UUID userId);

    Optional<ExamAttempt> findFirstByUserIdAndExamIdAndStatus(UUID userId, Integer examId, AttemptStatus status);

    /**
     * The attempt locked until the transaction ends, so answers and submits of one attempt apply one after another: a
     * second submit then sees the attempt already submitted.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ExamAttempt a where a.id = :id")
    Optional<ExamAttempt> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Opens an attempt unless the learner already has one in progress for the exam (uq_exam_attempt_in_progress). A
     * concurrent start waits for the other insert and then does nothing, instead of failing the transaction.
     *
     * @return 1 if the attempt was opened, 0 if one was already in progress
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into exam_attempt (id, user_id, exam_id, status, started_at, updated_at)
            values (:id, :userId, :examId, 'IN_PROGRESS', :startedAt, :startedAt)
            on conflict do nothing
            """, nativeQuery = true)
    int insertIfNoneInProgress(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("examId") Integer examId,
            @Param("startedAt") Instant startedAt);
}
