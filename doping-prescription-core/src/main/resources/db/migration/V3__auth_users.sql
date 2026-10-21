-- Module: auth
create table users (
    id                      uuid         not null,
    slug                    varchar(32)  not null,
    display_name            varchar(100) not null,
    role                    varchar(16)  not null,
    target_exam             varchar(8)   not null,
    exam_date               date,
    personalization_enabled boolean      not null default true,
    created_at              timestamptz  not null default now(),
    updated_at              timestamptz  not null default now(),
    constraint pk_users primary key (id),
    constraint uq_users_slug unique (slug),
    constraint ck_users_role check (role in ('STUDENT', 'ADMIN')),
    constraint ck_users_target_exam check (target_exam in ('TYT'))
);
