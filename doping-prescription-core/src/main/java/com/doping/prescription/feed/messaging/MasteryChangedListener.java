package com.doping.prescription.feed.messaging;

import com.doping.prescription.feed.application.QueueInvalidation;
import com.doping.prescription.learner.api.MasteryChanged;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Reads learner.mastery-changed into the learner's api event and hands it to {@link QueueInvalidation}. */
@Component
class MasteryChangedListener {

    private final QueueInvalidation invalidation;
    private final JsonMapper json;

    MasteryChangedListener(QueueInvalidation invalidation, JsonMapper json) {
        this.invalidation = invalidation;
        this.json = json;
    }

    @KafkaListener(topics = MasteryChanged.TOPIC, groupId = "feed")
    void onMasteryChanged(ConsumerRecord<String, byte[]> record) {
        MasteryChanged event = json.readValue(record.value(), MasteryChanged.class);
        invalidation.onMasteryChanged(event.eventId(), event.userId());
    }
}
