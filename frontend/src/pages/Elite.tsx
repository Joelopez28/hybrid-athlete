import { useState } from "react";
import { api } from "../api/client";
import type { Athlete } from "../api/types";
import { DISCIPLINE_LABELS } from "../api/types";
import { useElite } from "../hooks/useElite";

interface Props {
  athlete: Athlete;
}

function formatTarget(value: number, unit: string): string {
  if (unit === "segundos") {
    const total = Math.round(value);
    const hours = Math.floor(total / 3600);
    const minutes = Math.floor((total % 3600) / 60);
    const seconds = total % 60;
    return hours > 0
      ? `${hours}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`
      : `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
  }
  return `${value.toLocaleString("es-CL", { maximumFractionDigits: 2 })} ${unit}`;
}

export function Elite({ athlete }: Props) {
  const { status, loading, error, reload } = useElite();
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  async function handleApply() {
    setBusy(true);
    setActionError(null);
    setNotice(null);
    try {
      const updated = await api.applyElite();
      setNotice(`Clasificación confirmada: ${updated.tier === "ELITE" ? "Ruky Elite" : "Open"}.`);
      reload();
    } catch (cause) {
      setActionError(cause instanceof Error ? cause.message : "No se pudo solicitar la clasificación");
    } finally {
      setBusy(false);
    }
  }

  if (loading && !status) {
    return (
      <div className="elite-page">
        <div className="page-heading">
          <div>
            <p className="eyebrow">Clasificación</p>
            <h1>Ruky Elite</h1>
          </div>
        </div>
        <div className="board-skeleton" role="status" aria-label="Cargando clasificación">
          <span />
          <span />
        </div>
      </div>
    );
  }

  if (error && !status) {
    return (
      <div className="elite-page">
        <div className="inline-alert" role="alert">
          <p>{error}</p>
          <button type="button" className="secondary-button" onClick={reload}>
            Reintentar
          </button>
        </div>
      </div>
    );
  }

  if (!status) {
    return null;
  }

  const isElite = status.tier === "ELITE";
  const totalStandards = status.standards.length;
  const progress = totalStandards > 0 ? (status.metCount / totalStandards) * 100 : 0;

  return (
    <div className="elite-page">
      <header className="page-heading">
        <div>
          <p className="eyebrow">Clasificación</p>
          <h1>Ruky Elite</h1>
          <p className="muted">
            Cuatro estándares. Necesitás cumplir {status.requiredCount} para clasificar.
          </p>
        </div>
        <span className={`elite-tier-pill ${isElite ? "is-elite" : ""}`}>
          {isElite ? "Ruky Elite" : "Open"}
        </span>
      </header>

      {actionError && (
        <div className="inline-alert" role="alert">
          <p>{actionError}</p>
        </div>
      )}
      {notice && (
        <p className="form-message success" role="status">
          {notice}
        </p>
      )}

      <section className={`elite-summary panel ${status.qualified ? "is-qualified" : ""}`}>
        <div>
          <p className="eyebrow">Tu estado</p>
          <h2>
            {status.metCount} de {totalStandards} estándares
          </h2>
          <p>{status.message}</p>
        </div>
        <div className="elite-progress">
          <div className="elite-progress-track">
            <div className="elite-progress-fill" style={{ width: `${progress}%` }} />
          </div>
          <span>{Math.round(progress)}%</span>
        </div>
        {!isElite && (
          <button
            type="button"
            className="primary-button"
            onClick={handleApply}
            disabled={!status.qualified || busy}
          >
            {busy ? "Solicitando…" : status.qualified ? "Solicitar clasificación" : "Aún no clasificás"}
          </button>
        )}
        {isElite && (
          <p className="elite-note">
            Ya estás en la división. Mantené tus marcas para la temporada.
          </p>
        )}
      </section>

      <section className="standards-grid" aria-label="Estándares Ruky Elite">
        {status.standards.map((standard) => (
          <article
            className={`standard-card ${standard.met ? "is-met" : ""}`}
            key={standard.code}
          >
            <div className="standard-head">
              <span className={`discipline-tag discipline-${standard.discipline.toLowerCase()}`}>
                {DISCIPLINE_LABELS[standard.discipline]}
              </span>
              <span className="standard-state">{standard.met ? "Cumplido" : "Pendiente"}</span>
            </div>
            <h3>{standard.name}</h3>
            <p className="standard-detail">{standard.detail}</p>
            <div className="standard-progress">
              <div
                className="standard-progress-fill"
                style={{ width: `${Math.round(standard.progress * 100)}%` }}
              />
            </div>
            <div className="standard-meta">
              <span>Objetivo: {formatTarget(standard.target, standard.unit)}</span>
              <span>{Math.round(standard.progress * 100)}%</span>
            </div>
          </article>
        ))}
      </section>

      <section className="panel elite-rules" aria-labelledby="elite-rules-title">
        <p className="eyebrow">Cómo funciona</p>
        <h2 id="elite-rules-title">Estándares, no invitación</h2>
        <p>
          La clasificación automática se calcula sobre tus últimas 52 semanas. Necesitás{" "}
          {status.requiredCount} de {status.standards.length} estándares. La división Ruky Elite
          existente por invitación se mantiene; los estándares son configurables en
          <code>api.elite_standards</code>.
        </p>
        <p className="muted small">
          Atleta actual: {athlete.displayName} · {athlete.tier === "ELITE" ? "Ruky Elite" : "Open"}.
        </p>
      </section>
    </div>
  );
}
