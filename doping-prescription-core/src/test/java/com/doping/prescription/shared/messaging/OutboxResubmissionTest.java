package com.doping.prescription.shared.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.events.IncompleteEventPublications;

class OutboxResubmissionTest {

    @Test
    void resubmitsOnlyPublicationsOlderThanTheConfiguredAge() {
        // Given
        IncompleteEventPublications incomplete = mock(IncompleteEventPublications.class);
        Duration resubmitAfter = Duration.ofMinutes(3);
        OutboxResubmission resubmission =
                new OutboxResubmission(incomplete, new OutboxProperties(true, resubmitAfter, Duration.ofMinutes(1)));

        // When
        resubmission.resubmitIncomplete();

        // Then: younger publications may still be on their way to Kafka
        verify(incomplete).resubmitIncompletePublicationsOlderThan(resubmitAfter);
    }
}
