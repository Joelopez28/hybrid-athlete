-- Seed only on an empty database so re-running the init scripts is harmless.
do $$
begin
    if not exists (select 1 from api.tasks) then
        insert into api.tasks (title, description, status, priority, due_date)
        values
            ('Preparar la demo del viernes',
             'Mostrar el flujo completo React → Spring Boot → PostgREST.',
             'IN_PROGRESS', 'HIGH', current_date + 3),
            ('Documentar el contrato de /api',
             'Endpoints, errores y ejemplos de curl.',
             'TODO', 'MEDIUM', current_date + 7),
            ('Configurar CI',
             'GitHub Actions: mvn test + npm run build.',
             'TODO', 'LOW', null),
            ('Revisar accesibilidad del tablero',
             'Teclado, foco visible y contraste AA.',
             'DONE', 'MEDIUM', null);
    end if;
end
$$;
