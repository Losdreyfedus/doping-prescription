-- Module: feed

-- Videos a learner saved from a Dopingle card (the bookmark); "Kaydedilen" opens them as a feed, newest first.
-- content_id refers to the content module: no foreign key across modules (ADR 0005).
create table saved_content (
    user_id    uuid        not null,
    content_id integer     not null,
    created_at timestamptz not null,
    constraint pk_saved_content primary key (user_id, content_id)
);

create index ix_saved_content_user_created on saved_content (user_id, created_at);
