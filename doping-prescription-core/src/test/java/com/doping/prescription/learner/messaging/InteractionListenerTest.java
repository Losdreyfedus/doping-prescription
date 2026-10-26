package com.doping.prescription.learner.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.doping.prescription.feed.api.InteractionRecorded;
import com.doping.prescription.learner.messaging.InteractionListener.Interaction;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * The learner reads feed.interaction-recorded into its own record to avoid a module cycle (docs/api.md); this keeps
 * that copy of the contract in step with the feed's event.
 */
class InteractionListenerTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void readsTheFieldsItNeedsFromTheFeedsEvent() {
        // Given
        InteractionRecorded published = new InteractionRecorded(
                UUID.randomUUID(),
                InteractionRecorded.SCHEMA_VERSION,
                Instant.parse("2026-10-22T09:00:00Z"),
                UUID.randomUUID(),
                UUID.randomUUID(),
                34,
                9,
                "VIEW_COMPLETE",
                36_000);

        // When
        Interaction read = json.readValue(json.writeValueAsBytes(published), Interaction.class);

        // Then
        assertThat(InteractionListener.TOPIC).isEqualTo(InteractionRecorded.TOPIC);
        assertThat(read)
                .isEqualTo(new Interaction(
                        published.eventId(),
                        published.userId(),
                        published.topicId(),
                        published.interaction(),
                        published.occurredAt()));
    }
}
