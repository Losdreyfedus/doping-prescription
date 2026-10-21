package com.doping.prescription.shared.idempotency;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Consumer-side idempotency (docs/mimari.md §3): a consumer records each event id in the same transaction as its work,
 * so a redelivered event is recognised and skipped.
 */
@Component
public class ProcessedEvents {

    private static final String RECORD = """
            insert into processed_event (consumer, event_id, processed_at)
            values (:consumer, :eventId, :processedAt)
            on conflict (consumer, event_id) do nothing
            """;

    private final JdbcClient jdbc;
    private final Clock clock;

    ProcessedEvents(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /**
     * Records the event for the consumer; must run inside the consumer's transaction.
     *
     * @return true the first time, false if this consumer already processed the event
     */
    public boolean firstDelivery(String consumer, UUID eventId) {
        int inserted = jdbc.sql(RECORD)
                .param("consumer", consumer)
                .param("eventId", eventId)
                .param("processedAt", OffsetDateTime.now(clock))
                .update();
        return inserted == 1;
    }
}
