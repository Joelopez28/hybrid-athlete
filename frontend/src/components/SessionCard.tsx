import { useState } from "react";
import type { SessionEntry, TrainingSession } from "../api/types";
import {
  DISCIPLINE_LABELS,
  DIVISION_LABELS,
  SESSION_TYPE_LABELS,
  WOD_FORMAT_LABELS,
} from "../api/types";
import { formatFullDate, formatMinutes, formatVolume, relativeDay } from "../format";

interface Props {
  session: TrainingSession;
  compact?: boolean;
  onDelete?: (id: string) => Promise<void>;
}

function formatWeight(value: number): string {
  return Number(value).toLocaleString("es-CL", { maximumFractionDigits: 1 });
}

function setLabel(entry: SessionEntry): string {
  const seconds = entry.seconds != null ? `${formatWeight(entry.seconds)} s` : null;
  const weight = entry.weightKg != null ? `${formatWeight(entry.weightKg)} kg` : null;
  if (seconds && entry.reps == null) {
    return weight ? `${seconds} + ${weight}` : seconds;
  }
  if (entry.reps != null && weight) {
    return `${entry.reps} × ${weight}`;
  }
  if (entry.reps != null) {
    return `${entry.reps} reps`;
  }
  return seconds ?? weight ?? "—";
}

function groupEntries(entries: SessionEntry[]): Map<string, SessionEntry[]> {
  const groups = new Map<string, SessionEntry[]>();
  for (const entry of entries) {
    const list = groups.get(entry.movement) ?? [];
    list.push(entry);
    groups.set(entry.movement, list);
  }
  return groups;
}

export function SessionCard({ session, compact = false, onDelete }: Props) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const groups = groupEntries(session.entries);
  const hasEntries = groups.size > 0;
  const relative = relativeDay(session.performedOn);
  const result =
    session.wodScore != null
      ? `${session.wodFormat ? WOD_FORMAT_LABELS[session.wodFormat] : "Resultado"} · ${session.wodScore}`
      : null;

  async function handleDelete() {
    if (!onDelete) {
      return;
    }
    if (!window.confirm(`¿Eliminar "${session.title}"? No se puede deshacer.`)) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await onDelete(session.id);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "No se pudo eliminar");
      setBusy(false);
    }
  }

  return (
    <article
      className={`session-card discipline-${session.discipline.toLowerCase()} ${compact ? "compact" : ""}`}
      aria-busy={busy}
    >
      <div className="session-top">
        <div className="session-date">
          <span>{relative}</span>
          {(relative === "Hoy" || relative === "Ayer") && (
            <small>{formatFullDate(session.performedOn)}</small>
          )}
        </div>
        <div className="session-tags">
          <span className={`discipline-tag discipline-${session.discipline.toLowerCase()}`}>
            {DISCIPLINE_LABELS[session.discipline]}
          </span>
          <span className="type-tag">{SESSION_TYPE_LABELS[session.sessionType]}</span>
          {session.wodDivision && (
            <span className="division-tag">{DIVISION_LABELS[session.wodDivision]}</span>
          )}
        </div>
      </div>

      <h3 className="session-title">{session.title}</h3>

      <div className="session-meta">
        {session.durationMin != null && <span>{formatMinutes(session.durationMin)}</span>}
        {session.rpe != null && <span>RPE {formatWeight(session.rpe)}</span>}
        {session.volumeKg > 0 && <span>{formatVolume(session.volumeKg)}</span>}
        {result && <span className="session-result">{result}</span>}
      </div>

      {session.notes && <p className="session-notes">{session.notes}</p>}

      {hasEntries && !compact && (
        <details className="session-details">
          <summary>
            Ver series <span>{session.entries.length}</span>
          </summary>
          <div className="entry-groups">
            {[...groups.entries()].map(([movement, entries]) => (
              <div className="entry-group" key={movement}>
                <p className="entry-movement">{movement}</p>
                <div className="entry-sets">
                  {entries.map((entry) => (
                    <span className="set-pill" key={entry.id}>
                      {setLabel(entry)}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </details>
      )}

      {error && (
        <p className="inline-error" role="alert">
          {error}
        </p>
      )}

      {onDelete && !compact && (
        <button type="button" className="text-button danger" onClick={handleDelete} disabled={busy}>
          {busy ? "Eliminando…" : "Eliminar"}
        </button>
      )}
    </article>
  );
}
