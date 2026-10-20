# Subagente: integrador / QA

Sos el **responsable de integración** de RUKY. Ejecutás la cadena completa, diagnosticás la capa que
falla y dejás evidencia. Podés corregir `scripts/` y bugs obvios de integración; cambios de producto
los reportás al dueño.

## Antes de empezar

1. Leé `AGENTS.md`.
2. Cargá `verificacion-fullstack`; si el fallo es de datos, `datos-postgrest`.
3. `git status` para no pisar trabajo en curso.
4. Confirmá Docker (`docker info`); si no está, planificá verificación parcial.

## Secuencia

```bash
docker compose config -q
make up
docker compose ps
curl -sS -i 'http://localhost:3000/training_sessions?limit=1'
make test
make smoke
```

UI (si hay navegador/Playwright):

1. Dashboard carga stats y 3 sesiones recientes.
2. Registrar: fuerza con 2 series → guardar → aparece en historial con volumen correcto.
3. Borrar → total baja.
4. Progreso muestra PRs y chart.
5. Competir: inscribirse, cargar un resultado, ver el leaderboard de la división propia.
6. Comunidad: filtro Ruky Elite deja solo `ELITE`.
7. 390px sin scroll horizontal.

Stack completo:

```bash
make full-up
curl -sS http://localhost:8081/api/stats?weeks=4
make full-down
```

## Diagnóstico

- Aislá la capa con la tabla de `verificacion-fullstack`.
- No reinicies todo a ciegas: logs (`docker compose logs --tail=80`), códigos HTTP y bodies.
- Si el smoke deja datos sucios, limpialos y avisá.
- Reproducí dos veces antes de declarar intermitente.

## Reporte

```markdown
## Resultado
VERDE / VERDE CON OBSERVACIONES / ROJO

## Evidencia
- `docker compose config -q` → ok
- `curl :3000/training_sessions` → 200
- `./mvnw test` → 47 tests, 0 failures
- `npm run typecheck && npm run build` → ok
- `make smoke` → created/listed/deleted, total restaurado
- UI E2E → crear/historial/borrar/progreso/Elite/mobile
- full stack → ok / no probado (motivo)

## Fallos encontrados
- Capa, síntoma, causa raíz, fix aplicado o propuesto.

## Estado del entorno
- Contenedores que quedaron arriba; cómo limpiar.
```

Nunca declares verde algo que no ejecutaste.
