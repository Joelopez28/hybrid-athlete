-- Demo community and training history. Safe to re-run: only seeds when empty.

insert into api.athletes (handle, display_name, sex, birth_date, bodyweight_kg, country, bio, tier, disciplines)
values
    ('ruky',  'Ruky',  'MALE',   date '1994-06-12', 82.0, 'CL',
     'CrossFit, powerlifting y HYROX. Buscando entrar a Ruky Elite.', 'ELITE',
     array['CROSSFIT', 'POWERLIFTING', 'CALISTHENICS', 'HYROX']::text[]),
    ('vale',  'Vale',  'FEMALE', date '1996-03-08', 63.0, 'AR',
     'CrossFit RX y HYROX Open. Compite desde 2021.', 'ELITE',
     array['CROSSFIT', 'HYROX']::text[]),
    ('nacho', 'Nacho', 'MALE',   date '1990-11-21', 90.0, 'UY',
     'Powerlifting raw. Sentadilla y peso muerto, sin atajos.', 'OPEN',
     array['POWERLIFTING', 'CROSSFIT']::text[]),
    ('sofi',  'Sofi',  'FEMALE', date '1999-01-30', 70.0, 'CL',
     'Calistenia y skills. Trabajando front lever y dominadas lastradas.', 'OPEN',
     array['CALISTHENICS', 'CROSSFIT']::text[])
on conflict (handle) do nothing;

do $$
declare
    athlete      record;
    exercise     record;
    session_id   uuid;
    week         int;
    session_date date;
    ratio        numeric;
begin
    if exists (select 1 from api.training_sessions) then
        return;
    end if;

    -- Six weeks of progressive strength work for every athlete.
    for athlete in select * from api.athletes loop
        for week in 0..5 loop
            session_date := current_date - (week * 7);
            session_id := gen_random_uuid();

            insert into api.training_sessions
                (id, athlete_id, performed_on, discipline, session_type, title, duration_min, rpe)
            values
                (session_id, athlete.id, session_date, 'POWERLIFTING', 'STRENGTH',
                 'Fuerza · ' || to_char(session_date, 'DD/MM'), 70, 7.5);

            for exercise in
                select * from api.exercises where slug in ('back-squat', 'bench-press', 'deadlift')
            loop
                ratio := case exercise.slug
                    when 'back-squat' then 1.40
                    when 'bench-press' then 1.00
                    else 1.80
                end;

                insert into api.session_entries
                    (session_id, exercise_id, movement, set_index, reps, weight_kg, rpe)
                select session_id, exercise.id, exercise.name, sets.i, 5,
                       round((athlete.bodyweight_kg * ratio * (1 + (6 - week) * 0.02))::numeric, 2),
                       7.5
                from generate_series(1, 3) as sets(i);
            end loop;
        end loop;
    end loop;

    -- Extra Ruky sessions to make progress, WODs and HYROX visible in the demo.
    session_id := gen_random_uuid();
    insert into api.training_sessions
        (id, athlete_id, performed_on, discipline, session_type, title, notes, duration_min, rpe,
         wod_format, wod_score, wod_division)
    select session_id, id, current_date - 1, 'CROSSFIT', 'WOD', 'WOD "Fran"',
           '21-15-9 thrusters 43 kg + pull-ups. Uní broken sets en los thrusters.',
           12, 9.0, 'FOR_TIME', '04:20', 'RX'
    from api.athletes where handle = 'ruky';

    insert into api.session_entries (session_id, exercise_id, movement, set_index, reps, weight_kg)
    select session_id, id, 'Thruster', 1, 45, 43
    from api.exercises where slug = 'thruster';

    insert into api.session_entries (session_id, exercise_id, movement, set_index, reps)
    select session_id, id, 'Pull-up', 1, 45
    from api.exercises where slug = 'pull-up';

    session_id := gen_random_uuid();
    insert into api.training_sessions
        (id, athlete_id, performed_on, discipline, session_type, title, notes, duration_min, rpe,
         wod_format, wod_score, wod_division)
    select session_id, id, current_date - 10, 'CROSSFIT', 'WOD', 'WOD "Grace"',
           '30 clean & jerk a 60 kg. Cerrado en dos tandas.', 8, 8.5, 'FOR_TIME', '02:45', 'RX'
    from api.athletes where handle = 'ruky';

    insert into api.session_entries (session_id, exercise_id, movement, set_index, reps, weight_kg)
    select session_id, id, 'Clean', 1, 30, 60
    from api.exercises where slug = 'clean';

    session_id := gen_random_uuid();
    insert into api.training_sessions
        (id, athlete_id, performed_on, discipline, session_type, title, notes, duration_min, rpe,
         wod_format, wod_score, wod_division)
    select session_id, id, current_date - 17, 'HYROX', 'HYROX_SIM', 'HYROX sim · Open',
           'Simulación completa en el box: 8 km de carrera + 8 estaciones.',
           78, 8.0, 'FOR_TIME', '1:18:40', 'OPEN'
    from api.athletes where handle = 'ruky';

    session_id := gen_random_uuid();
    insert into api.training_sessions
        (id, athlete_id, performed_on, discipline, session_type, title, notes, duration_min, rpe)
    select session_id, id, current_date - 5, 'CALISTHENICS', 'SKILL', 'Skills · front lever y lastradas',
           'Buen control en front lever; sumar volumen de dominadas lastradas.', 50, 7.0
    from api.athletes where handle = 'ruky';

    insert into api.session_entries (session_id, exercise_id, movement, set_index, reps, weight_kg, rpe)
    select session_id, id, 'Weighted Pull-up', sets.i, 5, 20, 8
    from api.exercises, generate_series(1, 3) as sets(i)
    where slug = 'weighted-pull-up';

    insert into api.session_entries (session_id, exercise_id, movement, set_index, reps, weight_kg, rpe)
    select session_id, id, 'Dip', sets.i, 8, 10, 8
    from api.exercises, generate_series(1, 3) as sets(i)
    where slug = 'dip';

    insert into api.session_entries (session_id, exercise_id, movement, set_index, seconds, rpe)
    select session_id, id, 'Handstand Hold', sets.i, 45, 7
    from api.exercises, generate_series(1, 3) as sets(i)
    where slug = 'handstand-hold';

    insert into api.session_entries (session_id, exercise_id, movement, set_index, seconds, rpe)
    select session_id, id, 'Front Lever Hold', sets.i, 15, 8
    from api.exercises, generate_series(1, 3) as sets(i)
    where slug = 'front-lever-hold';
end
$$;
