package com.doping.prescription.learner.messaging;

import com.doping.prescription.exam.api.ExamResultPublished;
import com.doping.prescription.learner.application.LearnerProfileService;
import com.doping.prescription.learner.application.MasteryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Diagnosis step: every submitted exam updates the learner's topic mastery (docs/mimari.md §3). */
@Component
class ExamResultListener {

    private final MasteryService mastery;
    private final LearnerProfileService profiles;
    private final JsonMapper json;

    ExamResultListener(MasteryService mastery, LearnerProfileService profiles, JsonMapper json) {
        this.mastery = mastery;
        this.profiles = profiles;
        this.json = json;
    }

    @KafkaListener(topics = ExamResultPublished.TOPIC, groupId = "learner")
    void onExamResult(ConsumerRecord<String, byte[]> record) {
        // the raw record bypasses message conversion; the payload is the JSON written by the outbox
        ExamResultPublished result = json.readValue(record.value(), ExamResultPublished.class);
        mastery.applyExamResult(result);
        // after the commit, so readers see the new values at once
        profiles.refreshCache(result.userId());
    }
}
