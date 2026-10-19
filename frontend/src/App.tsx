import { useEffect, useState } from "react";
import type { CreateTaskInput } from "./api/types";
import { Pagination } from "./components/Pagination";
import { StatusFilter } from "./components/StatusFilter";
import { TaskForm } from "./components/TaskForm";
import { TaskList } from "./components/TaskList";
import { useTasks, type TaskFilter } from "./hooks/useTasks";

function App() {
  const [filter, setFilter] = useState<TaskFilter>("ALL");
  const [page, setPage] = useState(0);
  const { data, loading, error, reload, create, update, remove } = useTasks(filter, page);

  useEffect(() => {
    if (data && data.items.length === 0 && data.total > 0 && page > 0) {
      setPage((current) => Math.max(0, current - 1));
    }
  }, [data, page]);

  function handleFilter(next: TaskFilter) {
    setFilter(next);
    setPage(0);
  }

  async function handleCreate(input: CreateTaskInput) {
    const task = await create(input);
    setPage(0);
    return task;
  }

  const isEmpty = data !== null && data.items.length === 0;
  const emptyMessage =
    filter === "ALL"
      ? "Todavía no hay tareas. Creá la primera con el formulario."
      : "No hay tareas en este estado. Probá con otro filtro.";

  return (
    <div className="app">
      <header className="app-header">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">
            TF
          </span>
          <div>
            <p className="eyebrow">Starter full-stack</p>
            <h1>TaskFlow</h1>
          </div>
        </div>
        <p className="stack-pill">
          <span className="dot" aria-hidden="true" />
          Spring Boot · React · PostgREST
        </p>
      </header>

      <p className="intro">
        Un tablero mínimo pero completo: validación y contrato en Spring Boot, datos en PostgreSQL
        vía PostgREST y una UI tipada en React.
      </p>

      <main className="layout">
        <section className="composer panel" aria-labelledby="composer-title">
          <div className="panel-heading">
            <h2 id="composer-title">Nueva tarea</h2>
            <p className="muted">Se guarda directo en PostgreSQL.</p>
          </div>
          <TaskForm onCreate={handleCreate} />
        </section>

        <section className="tasks-panel panel" aria-labelledby="tasks-title">
          <div className="panel-heading tasks-heading">
            <h2 id="tasks-title">
              Tareas
              {loading && data && <span className="updating">Actualizando…</span>}
            </h2>
            <StatusFilter
              value={filter}
              total={data ? data.total : null}
              onChange={handleFilter}
            />
          </div>

          {error && (
            <div className="error-banner" role="alert">
              <p>{error}</p>
              <button type="button" className="secondary-button" onClick={reload}>
                Reintentar
              </button>
            </div>
          )}

          {loading && !data && (
            <div className="skeleton-list" role="status" aria-label="Cargando tareas">
              <div className="skeleton-card" />
              <div className="skeleton-card" />
              <div className="skeleton-card" />
            </div>
          )}

          {isEmpty && (
            <div className="empty-state">
              <span className="empty-mark" aria-hidden="true">
                ✓
              </span>
              <h3>Sin resultados</h3>
              <p>{emptyMessage}</p>
            </div>
          )}

          {data && data.items.length > 0 && (
            <TaskList
              tasks={data.items}
              onStatusChange={async (id, status) => {
                await update(id, { status });
              }}
              onDelete={remove}
            />
          )}

          {data && (
            <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
          )}
        </section>
      </main>

      <footer className="app-footer">
        <p>
            BFF en <code>/api</code> · PostgREST privado · PostgreSQL como fuente de verdad
        </p>
        <p className="muted">Starter generado para el equipo de agentes de TaskFlow.</p>
      </footer>
    </div>
  );
}

export default App;
