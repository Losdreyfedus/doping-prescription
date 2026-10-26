package com.doping.prescription.feed.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.doping.prescription.shared.idempotency.ProcessedEvents;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class QueueInvalidationTest {

    private final ProcessedEvents processedEvents = mock(ProcessedEvents.class);
    private final FeedStore store = mock(FeedStore.class);
    private final QueueInvalidation invalidation = new QueueInvalidation(processedEvents, store);

    @BeforeEach
    void startTransaction() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void endTransaction() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void dropsTheQueueOnceAndOnlyAfterTheCommit() {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(processedEvents.firstDelivery(QueueInvalidation.CONSUMER, eventId)).thenReturn(true, false);

        // When: the event arrives, Kafka redelivers it, then the transaction commits
        invalidation.onMasteryChanged(eventId, userId);
        invalidation.onMasteryChanged(eventId, userId);
        verify(store, never()).dropQueue(userId);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        // Then
        verify(store, times(1)).dropQueue(userId);
    }
}
