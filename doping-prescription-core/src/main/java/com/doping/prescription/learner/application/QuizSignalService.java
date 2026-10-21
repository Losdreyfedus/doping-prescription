package com.doping.prescription.learner.application;

import com.doping.prescription.learner.api.LearnerProfileQuery;
import com.doping.prescription.learner.api.LearnerSignalRecorder;
import com.doping.prescription.learner.domain.MasteryRounding;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Serves {@link LearnerSignalRecorder}; the cached profile is refreshed once the caller's transaction commits. */
@Service
class QuizSignalService implements LearnerSignalRecorder {

    private final MasteryService mastery;
    private final LearnerProfileService profiles;

    QuizSignalService(MasteryService mastery, LearnerProfileService profiles) {
        this.mastery = mastery;
        this.profiles = profiles;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public MasteryChange recordQuizAnswer(
            UUID userId, Integer topicId, boolean correct, String refId, Instant answeredAt) {
        MasteryChange change = mastery.applyQuizAnswer(userId, topicId, correct, refId, answeredAt);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                profiles.refreshCache(userId);
            }
        });
        return rounded(change);
    }

    @Override
    @Transactional(readOnly = true)
    public MasteryChange unchanged(UUID userId, Integer topicId) {
        double current = profiles.profile(userId).topics().stream()
                .filter(topic -> topic.topicId().equals(topicId))
                .mapToDouble(LearnerProfileQuery.TopicProfile::effectiveMastery)
                .findFirst()
                .orElseThrow();
        return rounded(new MasteryChange(topicId, current, current));
    }

    private static MasteryChange rounded(MasteryChange change) {
        return new MasteryChange(
                change.topicId(),
                MasteryRounding.forApi(change.before()).doubleValue(),
                MasteryRounding.forApi(change.after()).doubleValue());
    }
}
