---
name: frontend-react-ts
description: Implementar la UI de RUKY en React 19 + TypeScript + Vite: dashboard, registro de sesiones, historial, progreso y comunidad.
whenToUse: Al crear o modificar páginas, formularios, hooks, cliente API o estilos del frontend.
---

# Frontend React + TypeScript (RUKY)

## Mapa

```text
src/api/types.ts        Contrato espejo del backend (fuente de tipos)
src/api/client.ts       fetch tipado, query params, ApiError
src/hooks/useRukyData.ts   me + exercises + sessions + stats + leaderboard
src/hooks/useLeaderboard.ts fetch del ranking con filtros
src/components/Header.tsx  navegación y atleta
src/components/SessionForm.tsx  formulario dinámico por tipo
src/components/SessionCard.tsx  tarjeta + series desplegables
src/components/WeeklyVolumeChart.tsx
src/pages/              Dashboard, History, Progress, Compete, Community
src/App.tsx             tabs y composición
src/index.css           tokens, layout, dark UI
```

Dependencias: React 19, Vite 6, TypeScript 5.7. **Sin router, sin UI kit, sin librería de charts.**
Si la app crece, el orden es: router → TanStack Query → librería de UI, cada uno con ADR.

## Navegación y estado

- `App` mantiene `tab: "dashboard" | "log" | "history" | "progress" | "community"`.
- `useRukyData` carga todo en paralelo y expone `reload()`. Las mutaciones hacen `await` y `reload()`.
- `useLeaderboard` es independiente porque la comunidad cambia métrica/período/división.
- `useCompetition` carga la lista y el detalle de la primera competencia; las mutaciones hacen reload.
- No dupliques datos: `stats` viene del backend; no recalcules volumen ni PRs en React.

## Formulario de sesión (lo más delicado)

- `discipline` define el tipo por defecto y el catálogo de ejercicios visible.
- Tipos con series: `STRENGTH`, `SKILL`, `MIXED`. El resto usa resultado (`WOD`, `HYROX_SIM`).
- Serie: `exerciseId`, `reps`, `weightKg`, `seconds`. Holds usan segundos sin reps.
- Al cambiar de disciplina, reiniciá las series con un ejercicio válido.
- Validación de cliente: título, al menos una serie con reps/segundos, score en WOD/HYROX.
- El servidor manda: mostrá `ApiError.message` tal cual.
- Después de guardar: limpiar título/notas/score, dejar disciplina y fecha, y llamar `onSaved()`.

## Contrato y cliente

- `api/types.ts` debe coincidir con el backend; los campos nullable son opcionales porque el backend
  usa `NON_NULL`.
- Nunca armes URLs de PostgREST. Todo pasa por `/api`.
- `fetch` verifica `res.ok`, maneja 204 y convierte errores a `ApiError`.
- Cancelá efectos con `AbortController`; ignorá `AbortError`.

## Diseño

- Dark UI (`--bg #080b10`) con acento lima (`--accent #d8ff3e`), superficies `#101620`.
- Tipografía fuerte, títulos con tracking negativo, etiquetas uppercase pequeñas.
- Colores por disciplina: CrossFit naranja, powerlifting celeste, calistenia lima, HYROX violeta.
- Tokens en `:root`; no valores mágicos nuevos.
- Responsive: stats 4→2 columnas; grids 2→1; nav se envuelve en mobile.
- Estados obligatorios: loading, vacío, error con reintento.
- Foco visible y contraste AA; targets ≥ 40px.

## Pitfalls

1. La tabla de series tiene `min-width: 560px`; el contenedor debe permitir scroll horizontal
   (`min-width: 0` en el fieldset) o rompe el viewport móvil.
2. `entries` puede ser `[]`; no asumas que hay series.
3. `bestWeightKg`, `bestReps` y `bestSeconds` son mutuamente excluyentes en la práctica: mostrá el
   primero disponible, no “0 reps”.
4. Fechas `yyyy-MM-dd`: parsealas como locales (`T00:00:00`), no con `new Date("...")` y UTC.
5. El ranking de la comunidad se refetch al cambiar filtros; no filtres sólo en cliente si el backend
   puede hacerlo.
6. `window.confirm` se usa para borrar; si se reemplaza por modal, mantener foco y Escape.
7. En Competir, la tabla tiene scroll horizontal: la columna Total es sticky; no la saques.
8. El leaderboard debe abrir en la división del atleta (`myRegistration`), no en la primera por orden.
9. Los inputs de resultado son crudos: TIME en segundos, WEIGHT en kg, REPS en reps. El backend
   formatea el `displayScore`.

## Verificación

```bash
npm install
npm run dev        # :5173 con proxy /api → :8080
npm run typecheck
npm run build
```

Antes de cerrar: build verde y recorrido manual/E2E de registrar → historial → borrar → progreso →
comunidad. Probá 390px sin overflow horizontal.

## Definition of done

- [ ] Tipos espejo, sin `any`.
- [ ] Estados de carga/vacío/error y mensajes del servidor.
- [ ] Formulario usable con teclado y lector de pantalla.
- [ ] `npm run typecheck && npm run build` verdes; mobile revisado.
