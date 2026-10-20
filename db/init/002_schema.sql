-- RUKY training domain. Development schema: all tables live in `api` and are
-- exposed through PostgREST. Reset the volume after changing this file:
--   make db-reset

create table if not exists api.athletes (
    id             uuid primary key default gen_random_uuid(),
    handle         text not null unique check (handle ~ '^[a-z0-9_]{3,30}$'),
    display_name   text not null check (length(btrim(display_name)) between 2 and 80),
    sex            text not null default 'OTHER' check (sex in ('FEMALE', 'MALE', 'OTHER')),
    birth_date     date,
    bodyweight_kg  numeric(5, 1) check (bodyweight_kg between 30 and 250),
    country        text,
    bio            text check (length(bio) <= 500),
    tier           text not null default 'OPEN' check (tier in ('OPEN', 'ELITE')),
    disciplines    text[] not null default '{}'
        check (disciplines <@ array['CROSSFIT', 'POWERLIFTING', 'CALISTHENICS', 'HYROX']::text[]),
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now()
);

create table if not exists api.exercises (
    id           uuid primary key default gen_random_uuid(),
    slug         text not null unique,
    name         text not null,
    discipline   text not null check (discipline in ('CROSSFIT', 'POWERLIFTING', 'CALISTHENICS', 'HYROX')),
    category     text not null check (category in ('BARBELL', 'GYMNASTICS', 'METCON', 'HYROX_STATION', 'ACCESSORY', 'CARDIO')),
    unit         text not null default 'KG' check (unit in ('KG', 'BODYWEIGHT', 'REPS', 'SECONDS', 'METERS')),
    benchmark boolean not null default true,
    created_at   timestamptz not null default now()
);

create table if not exists api.training_sessions (
    id           uuid primary key default gen_random_uuid(),
    athlete_id   uuid not null references api.athletes (id) on delete cascade,
    performed_on date not null default current_date,
    discipline   text not null check (discipline in ('CROSSFIT', 'POWERLIFTING', 'CALISTHENICS', 'HYROX')),
    session_type text not null check (session_type in ('STRENGTH', 'WOD', 'HYROX_SIM', 'SKILL', 'CARDIO', 'MIXED')),
    title        text not null check (length(btrim(title)) between 1 and 120),
    notes        text check (length(notes) <= 2000),
    duration_min int check (duration_min between 1 and 600),
    rpe          numeric(3, 1) check (rpe between 1 and 10),
    wod_format   text check (wod_format in ('AMRAP', 'EMOM', 'FOR_TIME', 'INTERVAL')),
    wod_score    text check (length(wod_score) <= 40),
    wod_division text check (wod_division in ('RX', 'INTERMEDIATE', 'SCALED', 'OPEN', 'PRO', 'DOUBLES', 'RELAY')),
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);

create table if not exists api.session_entries (
    id          uuid primary key default gen_random_uuid(),
    session_id  uuid not null references api.training_sessions (id) on delete cascade,
    exercise_id uuid references api.exercises (id) on delete set null,
    movement    text not null check (length(btrim(movement)) between 1 and 120),
    set_index   int not null default 1 check (set_index between 1 and 100),
    reps        int check (reps between 1 and 1000),
    weight_kg   numeric(6, 2) check (weight_kg between 0 and 1000),
    seconds     numeric(7, 2) check (seconds between 0 and 7200),
    rpe         numeric(3, 1) check (rpe between 1 and 10),
    notes       text check (length(notes) <= 500),
    created_at  timestamptz not null default now()
);

create index if not exists sessions_athlete_date_idx
    on api.training_sessions (athlete_id, performed_on desc);
create index if not exists sessions_date_idx
    on api.training_sessions (performed_on desc);
create index if not exists entries_session_idx
    on api.session_entries (session_id, set_index);
create index if not exists exercises_discipline_idx
    on api.exercises (discipline, name);

create or replace function api.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists athletes_touch_updated_at on api.athletes;
create trigger athletes_touch_updated_at
    before update on api.athletes
    for each row execute function api.touch_updated_at();

drop trigger if exists sessions_touch_updated_at on api.training_sessions;
create trigger sessions_touch_updated_at
    before update on api.training_sessions
    for each row execute function api.touch_updated_at();

grant select, insert, update, delete on api.athletes to web_anon;
grant select, insert, update, delete on api.exercises to web_anon;
grant select, insert, update, delete on api.training_sessions to web_anon;
grant select, insert, update, delete on api.session_entries to web_anon;
