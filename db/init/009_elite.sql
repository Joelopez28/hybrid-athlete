-- Public, configurable standards for the Ruky Elite classification.
create table if not exists api.elite_standards (
    code        text primary key,
    discipline  text not null check (discipline in ('CROSSFIT', 'POWERLIFTING', 'CALISTHENICS', 'HYROX')),
    name        text not null,
    metric      text not null check (metric in ('TOTAL_RATIO', 'FRAN_TIME', 'HYROX_TIME', 'PULLUP_RATIO')),
    target      numeric(10, 2) not null check (target > 0),
    unit        text not null,
    description text,
    active      boolean not null default true,
    created_at  timestamptz not null default now()
);

insert into api.elite_standards (code, discipline, name, metric, target, unit, description)
values
    ('PL_TOTAL', 'POWERLIFTING', 'Total SBD', 'TOTAL_RATIO', 4.50, '× peso corporal',
     'Suma de tus mejores sentadilla, banco y peso muerto, relativa a tu peso corporal.'),
    ('CF_FRAN', 'CROSSFIT', 'Fran', 'FRAN_TIME', 270, 'segundos',
     'WOD Fran (21-15-9) en 4:30 o menos.'),
    ('HYROX_OPEN', 'HYROX', 'HYROX sim Open', 'HYROX_TIME', 5100, 'segundos',
     'Simulación HYROX completa en 1:25:00 o menos.'),
    ('CAL_PULLUP', 'CALISTHENICS', 'Dominada lastrada', 'PULLUP_RATIO', 0.20, '× peso corporal',
     'Lastre máximo en dominada equivalente al 20% de tu peso corporal.')
on conflict (code) do nothing;

grant select on api.elite_standards to app_user, app_service;
grant all on api.elite_standards to app_service;

notify pgrst, 'reload schema';
