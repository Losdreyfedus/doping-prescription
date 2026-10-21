-- Module: shared (owned by Spring Modulith)
-- Transactional outbox for domain events (docs/adr/0002-kafka-ve-outbox.md).
-- Copied verbatim from spring-modulith-events-jdbc 2.1.1,
-- org/springframework/modulith/events/jdbc/schemas/v2/schema-postgresql.sql.
-- Re-check this file whenever the Spring Modulith version changes.
create table event_publication (
    id                     uuid                     not null,
    listener_id            text                     not null,
    event_type             text                     not null,
    serialized_event       text                     not null,
    publication_date       timestamp with time zone not null,
    completion_date        timestamp with time zone,
    status                 text,
    completion_attempts    int,
    last_resubmission_date timestamp with time zone,
    primary key (id)
);

create index event_publication_serialized_event_hash_idx on event_publication using hash (serialized_event);
create index event_publication_by_completion_date_idx on event_publication (completion_date);
