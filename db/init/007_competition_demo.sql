-- Demo Strong Fit competition: 4 events, 2 active divisions and results for the community.
do $$
declare
    comp_id  uuid;
    elite_id uuid;
    rx_id    uuid;
begin
    if exists (select 1 from api.competitions) then
        return;
    end if;

    insert into api.competitions
        (slug, name, description, status, starts_on, ends_on, location)
    values
        ('strong-fit-ruky-1', 'Strong Fit RUKY #1',
         'Cuatro pruebas, dos divisiones y un solo leaderboard. Fuerza, motor y resistencia bajo fatiga.',
         'LIVE', current_date - 2, current_date + 5, 'Santiago, CL')
    returning id into comp_id;

    insert into api.competition_divisions (competition_id, name, tier, capacity)
    values (comp_id, 'Ruky Elite', 'ELITE', 32)
    returning id into elite_id;

    insert into api.competition_divisions (competition_id, name, tier, capacity)
    values (comp_id, 'RX', 'OPEN', 64)
    returning id into rx_id;

    insert into api.competition_events
        (competition_id, name, description, score_type, score_direction, order_index)
    values
        (comp_id, 'Fuerza total', 'Sentadilla + banco + peso muerto. Total en kg.',
         'WEIGHT', 'DESC', 1),
        (comp_id, 'Fran', '21-15-9 thrusters 43 kg + pull-ups. Tiempo total.',
         'TIME', 'ASC', 2),
        (comp_id, 'HYROX Sprint', 'Media simulación: 4 km + 4 estaciones. Tiempo total.',
         'TIME', 'ASC', 3),
        (comp_id, 'Max pull-ups', 'Repeticiones estrictas en un solo set.',
         'REPS', 'DESC', 4);

    insert into api.competition_registrations (competition_id, division_id, athlete_id)
    select comp_id,
           case when a.tier = 'ELITE' then elite_id else rx_id end,
           a.id
    from api.athletes a;

    insert into api.competition_results
        (competition_id, event_id, athlete_id, raw_score, display_score)
    select comp_id, e.id, a.id, seed.raw_score, seed.display_score
    from (values
        ('ruky',  'Fuerza total',  480,  '480 kg'),
        ('vale',  'Fuerza total',  330,  '330 kg'),
        ('nacho', 'Fuerza total',  520,  '520 kg'),
        ('sofi',  'Fuerza total',  300,  '300 kg'),
        ('ruky',  'Fran',          260,  '04:20'),
        ('vale',  'Fran',          300,  '05:00'),
        ('nacho', 'Fran',          330,  '05:30'),
        ('sofi',  'Fran',          360,  '06:00'),
        ('ruky',  'HYROX Sprint',  1120, '18:40'),
        ('vale',  'HYROX Sprint',  1200, '20:00'),
        ('nacho', 'HYROX Sprint',  1260, '21:00'),
        ('sofi',  'HYROX Sprint',  1320, '22:00'),
        ('ruky',  'Max pull-ups',  22,   '22 reps'),
        ('vale',  'Max pull-ups',  18,   '18 reps'),
        ('nacho', 'Max pull-ups',  20,   '20 reps'),
        ('sofi',  'Max pull-ups',  15,   '15 reps')
    ) as seed(handle, event_name, raw_score, display_score)
    join api.athletes a on a.handle = seed.handle
    join api.competition_events e
        on e.competition_id = comp_id and e.name = seed.event_name;
end
$$;

notify pgrst, 'reload schema';
