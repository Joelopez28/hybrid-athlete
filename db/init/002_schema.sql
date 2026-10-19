create table if not exists api.tasks (
    id          uuid primary key default gen_random_uuid(),
    title       text not null check (length(btrim(title)) between 1 and 200),
    description text check (length(description) <= 2000),
    status      text not null default 'TODO' check (status in ('TODO', 'IN_PROGRESS', 'DONE')),
    priority    text not null default 'MEDIUM' check (priority in ('LOW', 'MEDIUM', 'HIGH')),
    due_date    date,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

create index if not exists tasks_status_created_idx
    on api.tasks (status, created_at desc);

create or replace function api.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists tasks_touch_updated_at on api.tasks;
create trigger tasks_touch_updated_at
    before update on api.tasks
    for each row
    execute function api.touch_updated_at();

grant select, insert, update, delete on api.tasks to web_anon;
