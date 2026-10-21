package com.doping.prescription.learner.messaging;

import com.doping.prescription.learner.application.MasteryService;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps {@code last_served_at} current from feed interactions (docs/mimari.md §4). The feed already depends on the
 * learner's API, so the learner reads the message into its own record instead of importing the feed's event type: no
 * module cycle, and only the fields it needs.
 */
@Component
class InteractionListener {

    static final String TOPIC = "feed.interaction-recorded";

    private final MasteryService mastery;
    private final JsonMapper json;

    InteractionListener(MasteryService mastery, JsonMapper json) {
        this.mastery = mastery;
        this.json = json;
    }

    @KafkaListener(topics = TOPIC, groupId = "learner")
    void onInteraction(ConsumerRecord<String, byte[]> record) {
        Interaction event = json.readValue(record.value(), Interaction.class);
        mastery.recordServed(event.eventId(), event.userId(), event.topicId(), event.occurredAt());
    }

    /** The fields of feed.interaction-recorded the learner uses (docs/api.md). */
    record Interaction(UUID eventId, UUID userId, Integer topicId, Instant occurredAt) {}
}
