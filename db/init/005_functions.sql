-- Atomic session creation. PostgREST nested inserts are not available in this
-- deployment, so the BFF calls this function and then reads the session back.
create or replace function api.create_session(payload jsonb)
returns uuid
language plpgsql
as $$
declare
    new_id uuid;
    entry  jsonb;
begin
    insert into api.training_sessions
        (athlete_id, performed_on, discipline, session_type, title, notes,
         duration_min, rpe, wod_format, wod_score, wod_division)
    values (
        (payload ->> 'athlete_id')::uuid,
        coalesce((payload ->> 'performed_on')::date, current_date),
        payload ->> 'discipline',
        payload ->> 'session_type',
        btrim(payload ->> 'title'),
        nullif(btrim(coalesce(payload ->> 'notes', '')), ''),
        nullif(payload ->> 'duration_min', '')::int,
        nullif(payload ->> 'rpe', '')::numeric,
        nullif(payload ->> 'wod_format', ''),
        nullif(btrim(coalesce(payload ->> 'wod_score', '')), ''),
        nullif(payload ->> 'wod_division', '')
    )
    returning id into new_id;

    for entry in
        select * from jsonb_array_elements(coalesce(payload -> 'entries', '[]'::jsonb))
    loop
        insert into api.session_entries
            (session_id, exercise_id, movement, set_index, reps, weight_kg, seconds, rpe)
        values (
            new_id,
            nullif(entry ->> 'exercise_id', '')::uuid,
            entry ->> 'movement',
            coalesce(nullif(entry ->> 'set_index', '')::int, 1),
            nullif(entry ->> 'reps', '')::int,
            nullif(entry ->> 'weight_kg', '')::numeric,
            nullif(entry ->> 'seconds', '')::numeric,
            nullif(entry ->> 'rpe', '')::numeric
        );
    end loop;

    return new_id;
end;
$$;

grant execute on function api.create_session(jsonb) to web_anon;

-- Tell PostgREST about the new function as soon as it starts listening.
notify pgrst, 'reload schema';
