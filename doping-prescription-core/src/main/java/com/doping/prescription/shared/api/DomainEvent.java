package com.doping.prescription.shared.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Envelope of every cross-module event (docs/mimari.md §3, docs/api.md Event'ler). Consumers use {@link #eventId()} to
 * process each event once; the Kafka message key is {@link #userId()} so a learner's events stay in order.
 */
public interface DomainEvent {

    UUID eventId();

    int schemaVersion();

    Instant occurredAt();

    UUID userId();
}
