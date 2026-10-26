package com.doping.prescription.shared.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Resubmits event publications that did not complete (docs/mimari.md §3). Without it, an exam result written to the
 * outbox while Kafka was down would reach the learner's profile only after a restart.
 */
@Component
@ConditionalOnProperty(prefix = "prescription.messaging.outbox", name = "resubmit-enabled", havingValue = "true")
class OutboxResubmission {

    private final IncompleteEventPublications incomplete;
    private final OutboxProperties properties;

    OutboxResubmission(IncompleteEventPublications incomplete, OutboxProperties properties) {
        this.incomplete = incomplete;
        this.properties = properties;
    }

    @Scheduled(
            fixedDelayString = "${prescription.messaging.outbox.resubmit-interval}",
            initialDelayString = "${prescription.messaging.outbox.resubmit-interval}")
    void resubmitIncomplete() {
        incomplete.resubmitIncompletePublicationsOlderThan(properties.resubmitAfter());
    }
}
