package com.doping.prescription.feed.messaging;

import com.doping.prescription.feed.application.FeedService;
import com.doping.prescription.learner.api.MasteryChanged;
import com.doping.prescription.shared.idempotency.ProcessedEvents;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** A changed mastery makes the prepared queue stale; the next request rebuilds it (docs/algoritma.md §4). */
@Component
class MasteryChangedListener {

    static final String CONSUMER = "feed";

    private final FeedService feed;
    private final ProcessedEvents processedEvents;
    private final JsonMapper json;

    MasteryChangedListener(FeedService feed, ProcessedEvents processedEvents, JsonMapper json) {
        this.feed = feed;
        this.processedEvents = processedEvents;
        this.json = json;
    }

    @KafkaListener(topics = MasteryChanged.TOPIC, groupId = CONSUMER)
    void onMasteryChanged(ConsumerRecord<String, byte[]> record) {
        MasteryChanged event = json.readValue(record.value(), MasteryChanged.class);
        if (processedEvents.firstDelivery(CONSUMER, event.eventId())) {
            feed.invalidateQueue(event.userId());
        }
    }
}
