-- Auth + row level security for athlete isolation.
-- app_user: authenticated athlete (JWT role claim).
-- app_service: backend-only role for auth lookup/registration (BYPASSRLS).

create extension if not exists pgcrypto;

do $$
begin
    if not exists (select 1 from pg_roles where rolname = 'app_user') then
        create role app_user nologin;
    end if;
    if not exists (select 1 from pg_roles where rolname = 'app_service') then
        create role app_service nologin bypassrls;
    end if;
end
$$;

grant app_user, app_service to authenticator;
grant usage on schema api to app_user, app_service;

create table if not exists api.athlete_credentials (
    athlete_id    uuid primary key references api.athletes (id) on delete cascade,
    email         text not null,
    password_hash text not null,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    check (position('@' in email) > 1)
);

create unique index if not exists athlete_credentials_email_idx
    on api.athlete_credentials (lower(email));

create or replace function api.current_athlete_id()
returns uuid
language sql
stable
as $$
    select nullif(
        (nullif(current_setting('request.jwt.claims', true), '')::jsonb ->> 'sub'),
        ''
    )::uuid;
$$;

-- Athlete profiles are visible to authenticated athletes; only the owner updates theirs.
alter table api.athletes enable row level security;
drop policy if exists athletes_select on api.athletes;
create policy athletes_select on api.athletes
    for select to app_user using (true);
drop policy if exists athletes_update_own on api.athletes;
create policy athletes_update_own on api.athletes
    for update to app_user
    using (id = api.current_athlete_id())
    with check (id = api.current_athlete_id());

alter table api.training_sessions enable row level security;
drop policy if exists sessions_owner on api.training_sessions;
create policy sessions_owner on api.training_sessions
    for all to app_user
    using (athlete_id = api.current_athlete_id())
    with check (athlete_id = api.current_athlete_id());

alter table api.session_entries enable row level security;
drop policy if exists entries_owner on api.session_entries;
create policy entries_owner on api.session_entries
    for all to app_user
    using (exists (
        select 1 from api.training_sessions s
        where s.id = session_id and s.athlete_id = api.current_athlete_id()
    ))
    with check (exists (
        select 1 from api.training_sessions s
        where s.id = session_id and s.athlete_id = api.current_athlete_id()
    ));

alter table api.competition_registrations enable row level security;
drop policy if exists registrations_select on api.competition_registrations;
create policy registrations_select on api.competition_registrations
    for select to app_user using (true);
drop policy if exists registrations_own_write on api.competition_registrations;
create policy registrations_own_write on api.competition_registrations
    for all to app_user
    using (athlete_id = api.current_athlete_id())
    with check (athlete_id = api.current_athlete_id());

alter table api.competition_results enable row level security;
drop policy if exists results_select on api.competition_results;
create policy results_select on api.competition_results
    for select to app_user using (true);
drop policy if exists results_own_write on api.competition_results;
create policy results_own_write on api.competition_results
    for all to app_user
    using (athlete_id = api.current_athlete_id())
    with check (athlete_id = api.current_athlete_id());

-- Grants: app_user gets only what its policies allow.
revoke all on all tables in schema api from web_anon;
revoke execute on all functions in schema api from web_anon;

grant select on api.athletes, api.exercises, api.competitions,
    api.competition_divisions, api.competition_events,
    api.competition_registrations, api.competition_results to app_user;
grant update (display_name, sex, birth_date, bodyweight_kg, country, bio, disciplines)
    on api.athletes to app_user;
grant select, insert, update, delete on api.training_sessions, api.session_entries to app_user;
grant insert, update, delete on api.competition_registrations, api.competition_results to app_user;
grant execute on function api.create_session(jsonb) to app_user;

-- Service role is used only by the BFF for auth and never leaves the backend.
grant select, insert, update, delete on all tables in schema api to app_service;
grant execute on all functions in schema api to app_service;
grant select on all tables in schema api to app_service;

-- Community aggregates without exposing individual sessions.
create or replace view api.athlete_weekly_activity as
select
    s.athlete_id,
    date_trunc('week', s.performed_on)::date as week_start,
    count(distinct s.id)::int as sessions,
    coalesce(sum(s.duration_min), 0)::int as minutes,
    coalesce(sum(e.reps * e.weight_kg), 0)::numeric(12, 2) as volume_kg
from api.training_sessions s
left join api.session_entries e on e.session_id = s.id
group by s.athlete_id, date_trunc('week', s.performed_on)::date;

grant select on api.athlete_weekly_activity to app_user, app_service;

-- Atomic registration. Password is already BCrypt-hashed by the BFF.
create or replace function api.register_athlete(payload jsonb)
returns uuid
language plpgsql
security definer
set search_path = api, public
as $$
declare
    new_id uuid;
begin
    insert into api.athletes
        (handle, display_name, sex, bodyweight_kg, country, bio, tier, disciplines)
    values (
        lower(btrim(payload ->> 'handle')),
        btrim(payload ->> 'display_name'),
        coalesce(payload ->> 'sex', 'OTHER'),
        nullif(payload ->> 'bodyweight_kg', '')::numeric,
        nullif(btrim(coalesce(payload ->> 'country', '')), ''),
        nullif(btrim(coalesce(payload ->> 'bio', '')), ''),
        'OPEN',
        coalesce(
            (select array_agg(value) from jsonb_array_elements_text(
                coalesce(payload -> 'disciplines', '[]'::jsonb)
            )),
            '{}'::text[]
        )
    )
    returning id into new_id;

    insert into api.athlete_credentials (athlete_id, email, password_hash)
    values (
        new_id,
        lower(btrim(payload ->> 'email')),
        payload ->> 'password_hash'
    );

    return new_id;
end;
$$;

revoke all on function api.register_athlete(jsonb) from public;
grant execute on function api.register_athlete(jsonb) to app_service;

-- Demo credentials for the seeded athletes (dev only).
insert into api.athlete_credentials (athlete_id, email, password_hash)
select a.id, a.handle || '@ruky.demo', crypt('ruky1234', gen_salt('bf'))
from api.athletes a
on conflict (athlete_id) do nothing;

notify pgrst, 'reload schema';
