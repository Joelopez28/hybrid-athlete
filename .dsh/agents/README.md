# Briefs de subagentes (RUKY)

Cada archivo es un **prompt listo para pegar** en `subagent` (o `subagent_fork` si querés que herede
la conversación). No son agentes auto-registrados: son contratos de trabajo reutilizables.

## Uso

```text
subagent(
  description: "Backend: PRs por tiempo",
  prompt: "Leé .dsh/agents/backend-dev.md y .dsh/skills/backend-spring-boot/SKILL.md.
           Tarea: agregar PR de holds (segundos) a /api/stats con tests.
           No toques frontend. Reportá comandos y resultado."
)
```

## Reglas

- **Un dueño por área**: backend (`backend/`), frontend (`frontend/`), datos/infra (`db/`,
  `compose*.yaml`, Dockerfiles). Reviewer no edita.
- El brief ya incluye cargar `AGENTS.md` y su skill; no repitas todo el contexto.
- Si cambia el contrato, se actualiza `frontend/src/api/types.ts` y se avisa al humano.
- Paralelizá solo áreas independientes con el contrato ya fijado.
- Cierre esperado: archivos, comandos ejecutados, resultado y riesgos.

## Mapa

| Brief | Área | Skill |
|---|---|---|
| `data-dev.md` | `db/`, `compose*.yaml` | `datos-postgrest` |
| `backend-dev.md` | `backend/` | `backend-spring-boot` |
| `frontend-dev.md` | `frontend/` | `frontend-react-ts` |
| `reviewer.md` | solo lectura | `revision-codigo` |
| `integrator.md` | verificación/E2E | `verificacion-fullstack` |

## Orden recomendado

1. Humano + `arquitectura-fullstack`: fijar contrato y si toca schema/RPC.
2. `data-dev` (schema/RPC/seed) → `backend-dev` (endpoint/stats) → `frontend-dev` (UI) contra el
   contrato tipado.
3. `integrator`: `make test` + `make smoke` + recorrido UI.
4. `reviewer`: bloqueantes; el dueño aplica.

## Contexto de producto

RUKY registra sesiones de CrossFit, powerlifting, calistenia y HYROX, calcula volumen/PRs y muestra
un leaderboard de comunidad con división **Ruky Elite**. Auth y competencias Strong Fit son fase 2.
