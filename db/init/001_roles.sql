-- Dev-only roles. Replace the password and add JWT/RLS before exposing real data.
do $$
begin
  if not exists (select 1 from pg_roles where rolname = 'authenticator') then
    create role authenticator noinherit login password 'authenticator-dev-password';
  end if;

  if not exists (select 1 from pg_roles where rolname = 'web_anon') then
    create role web_anon nologin;
  end if;
end
$$;

-- PostgREST connects as authenticator and switches to web_anon for anonymous requests.
grant web_anon to authenticator;

create schema if not exists api;
grant usage on schema api to web_anon;
