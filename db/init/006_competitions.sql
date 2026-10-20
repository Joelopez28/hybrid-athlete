-- Strong Fit competitions: competitions, divisions, events, registrations and results.
-- PostgREST exposes these tables read/write in development; scoring lives in the BFF.

create table if not exists api.competitions (
    id          uuid primary key default gen_random_uuid(),
    slug        text not null unique check (slug ~ '^[a-z0-9-]+$'),
    name        text not null check (length(btrim(name)) between 2 and 120),
    description text check (length(description) <= 2000),
    status      text not null default 'DRAFT' check (status in ('DRAFT', 'LIVE', 'FINISHED')),
    starts_on   date,
    ends_on     date,
    location    text,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    check (ends_on is null or starts_on is null or ends_on >= starts_on)
);

create table if not exists api.competition_divisions (
    id             uuid primary key default gen_random_uuid(),
    competition_id uuid not null references api.competitions (id) on delete cascade,
    name           text not null check (length(btrim(name)) between 2 and 60),
    tier           text not null default 'OPEN' check (tier in ('OPEN', 'ELITE')),
    capacity       int check (capacity > 0),
    created_at     timestamptz not null default now(),
    unique (competition_id, name),
    unique (id, competition_id)
);

create table if not exists api.competition_events (
    id             uuid primary key default gen_random_uuid(),
    competition_id uuid not null references api.competitions (id) on delete cascade,
    name           text not null check (length(btrim(name)) between 2 and 120),
    description    text check (length(description) <= 1000),
    score_type     text not null check (score_type in ('TIME', 'REPS', 'WEIGHT')),
    score_direction text not null check (score_direction in ('ASC', 'DESC')),
    order_index    int not null default 0,
    created_at     timestamptz not null default now(),
    unique (competition_id, name),
    unique (id, competition_id)
);

create table if not exists api.competition_registrations (
    id             uuid primary key default gen_random_uuid(),
    competition_id uuid not null references api.competitions (id) on delete cascade,
    division_id    uuid not null,
    athlete_id     uuid not null references api.athletes (id) on delete cascade,
    status         text not null default 'REGISTERED' check (status in ('REGISTERED', 'WITHDRAWN')),
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now(),
    unique (competition_id, athlete_id),
    foreign key (division_id, competition_id)
        references api.competition_divisions (id, competition_id) on delete cascade
);

create table if not exists api.competition_results (
    id             uuid primary key default gen_random_uuid(),
    competition_id uuid not null,
    event_id       uuid not null,
    athlete_id     uuid not null references api.athletes (id) on delete cascade,
    raw_score      numeric(10, 2) not null check (raw_score >= 0),
    display_score  text not null check (length(display_score) <= 40),
    notes          text check (length(notes) <= 500),
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now(),
    unique (event_id, athlete_id),
    foreign key (event_id, competition_id)
        references api.competition_events (id, competition_id) on delete cascade
);

create index if not exists competition_divisions_comp_idx
    on api.competition_divisions (competition_id);
create index if not exists competition_events_comp_idx
    on api.competition_events (competition_id, order_index);
create index if not exists competition_registrations_comp_idx
    on api.competition_registrations (competition_id, division_id);
create index if not exists competition_results_comp_idx
    on api.competition_results (competition_id, event_id);

drop trigger if exists competitions_touch_updated_at on api.competitions;
create trigger competitions_touch_updated_at
    before update on api.competitions
    for each row execute function api.touch_updated_at();

drop trigger if exists competition_registrations_touch_updated_at on api.competition_registrations;
create trigger competition_registrations_touch_updated_at
    before update on api.competition_registrations
    for each row execute function api.touch_updated_at();

drop trigger if exists competition_results_touch_updated_at on api.competition_results;
create trigger competition_results_touch_updated_at
    before update on api.competition_results
    for each row execute function api.touch_updated_at();

grant select, insert, update, delete on api.competitions to web_anon;
grant select, insert, update, delete on api.competition_divisions to web_anon;
grant select, insert, update, delete on api.competition_events to web_anon;
grant select, insert, update, delete on api.competition_registrations to web_anon;
grant select, insert, update, delete on api.competition_results to web_anon;

notify pgrst, 'reload schema';
