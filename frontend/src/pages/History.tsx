import { useMemo, useState } from "react";
import type { Discipline, TrainingSession } from "../api/types";
import { DISCIPLINES, DISCIPLINE_LABELS } from "../api/types";
import { SessionCard } from "../components/SessionCard";
import type { Tab } from "../components/Header";

interface Props {
  sessions: TrainingSession[];
  onDelete: (id: string) => Promise<void>;
  onNavigate: (tab: Tab) => void;
}

type Filter = Discipline | "ALL";

export function History({ sessions, onDelete, onNavigate }: Props) {
  const [filter, setFilter] = useState<Filter>("ALL");

  const filtered = useMemo(
    () => (filter === "ALL" ? sessions : sessions.filter((session) => session.discipline === filter)),
    [filter, sessions],
  );

  return (
    <div className="history">
      <header className="page-heading">
        <div>
          <p className="eyebrow">Registro</p>
          <h1>Historial</h1>
          <p className="muted">
            {filtered.length} {filtered.length === 1 ? "sesión" : "sesiones"} en las últimas 50
            entradas.
          </p>
        </div>
        <button type="button" className="primary-button" onClick={() => onNavigate("log")}>
          Registrar sesión
        </button>
      </header>

      <div className="chip-row filter-row" role="group" aria-label="Filtrar por disciplina">
        <button
          type="button"
          className="choice-chip"
          aria-pressed={filter === "ALL"}
          onClick={() => setFilter("ALL")}
        >
          Todas
        </button>
        {DISCIPLINES.map((discipline) => (
          <button
            key={discipline}
            type="button"
            className={`choice-chip choice-${discipline.toLowerCase()}`}
            aria-pressed={filter === discipline}
            onClick={() => setFilter(discipline)}
          >
            {DISCIPLINE_LABELS[discipline]}
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <div className="empty-panel">
          <h2>No hay sesiones en este filtro</h2>
          <p>Registrá una sesión o probá con otra disciplina.</p>
          <button type="button" className="secondary-button" onClick={() => setFilter("ALL")}>
            Ver todas
          </button>
        </div>
      ) : (
        <div className="session-list">
          {filtered.map((session) => (
            <SessionCard key={session.id} session={session} onDelete={onDelete} />
          ))}
        </div>
      )}
    </div>
  );
}
