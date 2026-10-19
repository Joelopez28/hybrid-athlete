# Briefs de subagentes

Cada archivo es un **prompt listo para pegar** en `subagent` (o `subagent_fork` si querés que herede
la conversación). No son agentes auto-registrados por DSH: son contratos de trabajo reutilizables.

## Cómo usarlos

```text
subagent:
  description: "Backend: endpoint de comentarios"
  prompt: |
    Leé y seguí /ruta/al/repo/.dsh/agents/backend-dev.md.
    Tarea: agregar CRUD de comentarios por tarea, con validación y tests.
    Contexto: contrato propuesto ...
```

Reglas de convivencia:

- **Un dueño por área**: backend, frontend, datos/infra. El reviewer no edita.
- El brief ya incluye cargar `AGENTS.md` y la skill; no repitas todo el contexto en el prompt.
- Si el subagente cambia el contrato, debe avisarlo en su reporte y actualizar `frontend/src/api/types.ts`
  solo si el humano lo autorizó; si no, lo deja anotado.
- Paralelizá solo áreas independientes (backend y frontend pueden ir juntos **si el contrato ya está fijado**).
- Cierre esperado: archivos, comandos ejecutados, resultado y riesgos.

## Mapa

| Brief | Área | Skill |
|---|---|---|
| `data-dev.md` | `db/`, `compose*.yaml` | `datos-postgrest` |
| `backend-dev.md` | `backend/` | `backend-spring-boot` |
| `frontend-dev.md` | `frontend/` | `frontend-react-ts` |
| `reviewer.md` | solo lectura | `revision-codigo` |
| `integrator.md` | verificación y smoke | `verificacion-fullstack` |

## Orden recomendado para una feature

1. Humano + `arquitectura-fullstack`: fijar contrato y decisión.
2. `data-dev` (tabla/grants) y `backend-dev` en secuencia si el backend depende del esquema;
   `frontend-dev` en paralelo contra el contrato tipado.
3. `integrator`: `make test` + `make smoke`.
4. `reviewer`: informe; el dueño aplica bloqueantes.
