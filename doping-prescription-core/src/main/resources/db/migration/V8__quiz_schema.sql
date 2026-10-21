-- Module: quiz
-- user_id, micro_question_id (content) and impression_id (feed) belong to other modules:
-- indexed, no foreign keys (ADR 0005).
create table micro_quiz_attempt (
    id                uuid        not null,
    user_id           uuid        not null,
    micro_question_id integer     not null,
    impression_id     uuid        not null,
    selected_key      char(1)     not null,
    -- decided on the server; the correct key itself is never stored here or sent to the client before answering
    is_correct        boolean     not null,
    response_ms       integer     not null,
    points_awarded    integer     not null,
    created_at        timestamptz not null default now(),
    constraint pk_micro_quiz_attempt primary key (id),
    -- A second answer to the same question, or two concurrent requests, cannot both be stored;
    -- the violation is translated into "0 points" (docs/api.md). Also serves lookups by user_id.
    constraint uq_micro_quiz_attempt_user_question unique (user_id, micro_question_id),
    constraint ck_micro_quiz_attempt_selected_key check (selected_key in ('A', 'B', 'C', 'D')),
    constraint ck_micro_quiz_attempt_response_ms check (response_ms >= 0),
    constraint ck_micro_quiz_attempt_points check (points_awarded >= 0)
);

create index ix_micro_quiz_attempt_question on micro_quiz_attempt (micro_question_id);
create index ix_micro_quiz_attempt_impression on micro_quiz_attempt (impression_id);
