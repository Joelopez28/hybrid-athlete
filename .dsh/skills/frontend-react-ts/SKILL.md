---
name: frontend-react-ts
description: Implementar UI en React 19 + TypeScript + Vite contra el contrato /api. Cliente tipado, hooks, formularios accesibles y estados de carga/error.
whenToUse: Al crear o modificar componentes, hooks, cliente API, estilos o formularios del frontend.
---

# Frontend React + TypeScript

## Mapa

```text
src/
├── api/types.ts        Contrato espejo del backend (fuente de tipos)
├── api/client.ts       fetch tipado, query params, ApiError
├── hooks/useTasks.ts   estado remoto + mutaciones
├── components/         TaskForm, TaskList, TaskItem, StatusFilter, ...
├── App.tsx             composición de pantalla
└── index.css           tokens CSS, layout, componentes
```

Dependencias: React 19, Vite 6, TypeScript 5.7. **No agregues** librería de estado, router ni UI kit
sin una decisión en `docs/decisiones.md`. Si la app crece, el orden es: router → TanStack Query → UI kit.

## Contrato y cliente

`api/types.ts` debe coincidir **literalmente** con lo que devuelve Spring:

```ts
export const TASK_STATUSES = ["TODO", "IN_PROGRESS", "DONE"] as const;
export type TaskStatus = (typeof TASK_STATUSES)[number];

export interface Task {
  id: string;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: "LOW" | "MEDIUM" | "HIGH";
  dueDate: string | null;   // yyyy-MM-dd
  createdAt: string;        // ISO-8601
  updatedAt: string;
}
```

`api/client.ts` centraliza todo `fetch`:

```ts
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`/api${path}`, {
    headers: { "Content-Type": "application/json", ...init?.headers },
    ...init,
  });
  if (!res.ok) throw await toApiError(res);
  return res.status === 204 ? (undefined as T) : res.json();
}
```

- Nunca armes URLs de PostgREST ni uses `VITE_POSTGREST_URL`.
- Errores: parseá `{ message, status }`; mostrá `message`, nunca `[object Object]`.
- Listado: `?page&size&status`; el backend ya devuelve `{ items, total, ... }`.
- Cancelá al desmontar/cambiar filtro con `AbortController`; ignorá `AbortError`.

## Hook de datos (patrón único)

```ts
const [state, setState] = useState<{ status: "loading" | "ready" | "error"; data: Task[]; error?: string }>(...)
```

Un solo hook `useTasks(filter)` expone `{ items, total, loading, error, reload, createTask, updateTask, deleteTask }`.
Las mutaciones hacen `await` y luego `reload()`; nada de estados duplicados. Si agregás actualización
optimista, guardá el snapshot y revertí en error.

Reglas:
- `useEffect` solo para suscripciones/sincronización; el fetch inicial va ahí con cleanup.
- Dependencias honestas del efecto (`status`, `page`); sin `eslint-disable`.
- No derives estado que puedas calcular en render (`filtered = items.filter(...)`).
- Keys estables (`task.id`), nunca índices para listas reordenables.

## Formularios accesibles

- `<label htmlFor>` + `id`; errores con `aria-describedby` y `aria-invalid`.
- Validación de cliente solo para UX; el backend manda. Mostrá el error del servidor junto al campo
  cuando sea 400.
- Botón submit deshabilitado mientras `saving`; cancelar limpia el formulario.
- `dueDate` vacío → `null`, no `""`.
- Después de crear: limpiar, enfocar el primer campo y anunciar con `role="status"`.

## UI y estilos

- Tokens en `:root` (color, radius, spacing, sombra). Dark mode opcional vía `prefers-color-scheme`.
- Layout responsive con CSS grid; breakpoint principal 720px.
- Estados obligatorios: **cargando** (skeleton o texto), **vacío** (con acción), **error** (con reintento).
- Foco visible siempre (`:focus-visible`), targets ≥ 40px, contraste AA.
- Textos de UI en español; identificadores en inglés. Sin emojis decorativos en labels.

## Comandos y verificación

```bash
npm install
npm run dev        # :5173 con proxy /api → :8080
npm run typecheck  # tsc --noEmit
npm run build      # tsc -b && vite build
npm run preview    # servir dist
```

Antes de cerrar: `typecheck` + `build` verdes y prueba manual de crear/editar/borrar con el backend
levantado. Si no hay backend, al menos el build y el estado de error deben verse bien.

## Pitfalls

1. `fetch` no rechaza en 4xx/5xx: siempre `if (!res.ok)`.
2. `res.json()` en 204 lanza; contemplá `DELETE`.
3. Proxy de Vite solo aplica en dev; en producción Nginx enruta `/api` (ver `frontend/nginx.conf`).
4. `strict` + `noUncheckedIndexedAccess`: evitá `items[0]!`; usá `at(0)` con guard.
5. Fechas `yyyy-MM-dd` se corren de día si usás `new Date("2026-10-18")` y mostrás UTC; formateá con
   `Intl.DateTimeFormat("es", { timeZone: "UTC" })` o parseá como local.
6. No metas `dangerouslySetInnerHTML`; la descripción es texto plano.

## Definition of done

- [ ] Tipos espejo del contrato; sin `any` ni casts innecesarios.
- [ ] Estados de carga/vacío/error implementados y probados visualmente.
- [ ] Formulario usable solo con teclado y con lector de pantalla.
- [ ] `npm run typecheck && npm run build` verdes.
