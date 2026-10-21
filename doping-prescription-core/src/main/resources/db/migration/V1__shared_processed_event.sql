-- Module: shared
-- Idempotency for Kafka consumers: a consumer records each event id it has handled
-- in the same transaction as its work (docs/mimari.md §3).
create table processed_event (
    consumer     varchar(64) not null,
    event_id     uuid        not null,
    processed_at timestamptz not null,
    created_at   timestamptz not null default now(),
    constraint pk_processed_event primary key (consumer, event_id)
);
