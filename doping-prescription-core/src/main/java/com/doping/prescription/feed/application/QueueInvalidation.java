package com.doping.prescription.feed.application;

import com.doping.prescription.shared.idempotency.ProcessedEvents;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** A changed mastery makes the prepared queue stale; the next request rebuilds it (docs/algoritma.md §4). */
@Service
public class QueueInvalidation {

    static final String CONSUMER = "feed";

    private final ProcessedEvents processedEvents;
    private final FeedStore store;

    QueueInvalidation(ProcessedEvents processedEvents, FeedStore store) {
        this.processedEvents = processedEvents;
        this.store = store;
    }

    /**
     * Drops the learner's queue once per event (AGENTS.md rule 6). The event is recorded in the transaction and the
     * queue dropped after its commit, so a failed transaction leaves the event to be redelivered.
     */
    @Transactional
    public void onMasteryChanged(UUID eventId, UUID userId) {
        if (!processedEvents.firstDelivery(CONSUMER, eventId)) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                store.dropQueue(userId);
            }
        });
    }
}
