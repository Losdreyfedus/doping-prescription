-- Module: feed

-- A client event id is unique per learner, not globally: with a global key one learner could claim another learner's
-- id first and have their interaction silently dropped as a "duplicate" (AGENTS.md rule 6).
alter table feed_interaction drop constraint uq_feed_interaction_client_event;
alter table feed_interaction
    add constraint uq_feed_interaction_user_client_event unique (user_id, client_event_id);

-- The rewatch block (docs/algoritma.md §4, rule 2) reads the server-side receive time, never the client's clock;
-- client_ts is kept as reported but no longer queried.
drop index ix_feed_interaction_user_time;
create index ix_feed_interaction_user_created on feed_interaction (user_id, created_at);
