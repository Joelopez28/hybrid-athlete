import { useState } from "react";
import type { Athlete, AthleteTier, LeaderboardMetric } from "../api/types";
import { METRIC_LABELS } from "../api/types";
import { useLeaderboard } from "../hooks/useLeaderboard";
import { formatMinutes, formatNumber, formatVolume, initials } from "../format";

interface Props {
  athlete: Athlete;
}

function metricValue(value: number, metric: LeaderboardMetric): string {
  switch (metric) {
    case "VOLUME":
      return formatVolume(value);
    case "MINUTES":
      return formatMinutes(value);
    default:
      return formatNumber(value);
  }
}

export function Community({ athlete }: Props) {
  const [metric, setMetric] = useState<LeaderboardMetric>("VOLUME");
  const [weeks, setWeeks] = useState(4);
  const [tier, setTier] = useState<AthleteTier | null>(null);
  const { entries, loading, error, reload } = useLeaderboard(weeks, metric, tier);

  return (
    <div className="community-page">
      <header className="page-heading">
        <div>
          <p className="eyebrow">Comunidad</p>
          <h1>Ranking</h1>
          <p className="muted">
            Comparate por volumen, sesiones o minutos. Todo sale de entrenamientos registrados.
          </p>
        </div>
      </header>

      <section className="panel board-panel" aria-labelledby="ranking-title">
        <div className="board-controls">
          <div className="chip-row" role="group" aria-label="Métrica del ranking">
            {(Object.keys(METRIC_LABELS) as LeaderboardMetric[]).map((value) => (
              <button
                key={value}
                type="button"
                className="choice-chip"
                aria-pressed={metric === value}
                onClick={() => setMetric(value)}
              >
                {METRIC_LABELS[value]}
              </button>
            ))}
          </div>

          <div className="board-filters">
            <label className="select-label">
              Período
              <select value={weeks} onChange={(event) => setWeeks(Number(event.target.value))}>
                <option value={4}>4 semanas</option>
                <option value={8}>8 semanas</option>
                <option value={12}>12 semanas</option>
              </select>
            </label>

            <div className="segmented" role="group" aria-label="División">
              <button
                type="button"
                aria-pressed={tier === null}
                onClick={() => setTier(null)}
              >
                Todos
              </button>
              <button
                type="button"
                aria-pressed={tier === "ELITE"}
                onClick={() => setTier("ELITE")}
              >
                Ruky Elite
              </button>
            </div>
          </div>
        </div>

        {error && (
          <div className="inline-alert" role="alert">
            <p>{error}</p>
            <button type="button" className="secondary-button" onClick={reload}>
              Reintentar
            </button>
          </div>
        )}

        {loading && !entries && (
          <div className="board-skeleton" role="status" aria-label="Cargando ranking">
            <span />
            <span />
            <span />
            <span />
          </div>
        )}

        {entries && entries.length === 0 && (
          <div className="empty-panel">
            <h2>Sin actividad en este período</h2>
            <p>Cuando haya sesiones registradas, el ranking aparece acá.</p>
          </div>
        )}

        {entries && entries.length > 0 && (
          <ol className="leaderboard">
            {entries.map((entry) => (
              <li key={entry.athleteId} className={entry.me ? "is-me" : ""}>
                <span className="board-rank">{entry.rank}</span>
                <span className="avatar" aria-hidden="true">
                  {initials(entry.displayName)}
                </span>
                <span className="lb-name">
                  <strong>
                    {entry.displayName}
                    {entry.me && <em>vos</em>}
                  </strong>
                  <small>
                    @{entry.handle}
                    {entry.tier === "ELITE" && <span className="tier-badge tier-elite">Ruky Elite</span>}
                  </small>
                </span>
                <span className="lb-value">
                  <strong>{metricValue(entry.value, metric)}</strong>
                  <small>{entry.sessions} sesiones</small>
                </span>
              </li>
            ))}
          </ol>
        )}
      </section>

      <div className="community-grid">
        <section className="panel" aria-labelledby="elite-info-title">
          <p className="eyebrow">La división</p>
          <h2 id="elite-info-title">Ruky Elite</h2>
          <p>
            La categoría de élite de la comunidad. Por ahora se entra por invitación y se compite
            por ranking de temporada; los benchmarks clasificatorios se publican en la próxima fase.
          </p>
          <div className="elite-standards">
            <div>
              <span>Powerlifting</span>
              <strong>Total SBD ≥ 5× peso corporal</strong>
            </div>
            <div>
              <span>CrossFit</span>
              <strong>WODs RX + benchmark competitivo</strong>
            </div>
            <div>
              <span>HYROX</span>
              <strong>Open por debajo de 1:20</strong>
            </div>
            <div>
              <span>Calistenia</span>
              <strong>Skills avanzados + lastradas</strong>
            </div>
          </div>
          <p className="muted small">
            Estándares de referencia, no requisitos oficiales todavía. Tu perfil actual:{" "}
            {athlete.tier === "ELITE" ? "Ruky Elite" : "Open"}.
          </p>
        </section>

        <section className="panel competition-teaser" aria-labelledby="competition-title">
          <p className="eyebrow">En competencia</p>
          <h2 id="competition-title">Strong Fit RUKY #1</h2>
          <p>
            La primera competencia está en vivo: cuatro pruebas, divisiones Ruky Elite y RX, y
            puntaje por posición. Inscribite y cargá tus marcas desde la pestaña Competir.
          </p>
          <div className="teaser-row">
            <span>Formato</span>
            <strong>Evento + leaderboard</strong>
          </div>
          <div className="teaser-row">
            <span>Divisiones</span>
            <strong>RX · Scaled · Elite</strong>
          </div>
          <div className="teaser-row">
            <span>Estado</span>
            <strong>En vivo</strong>
          </div>
        </section>
      </div>
    </div>
  );
}
