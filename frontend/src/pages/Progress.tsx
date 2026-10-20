import type { Stats } from "../api/types";
import { DISCIPLINE_LABELS } from "../api/types";
import { StatCard } from "../components/StatCard";
import { WeeklyVolumeChart } from "../components/WeeklyVolumeChart";
import { formatDayMonth, formatMinutes, formatNumber, formatVolume } from "../format";

interface Props {
  stats: Stats;
}

function formatWeight(value: number): string {
  return Number(value).toLocaleString("es-CL", { maximumFractionDigits: 1 });
}

export function Progress({ stats }: Props) {
  const totalVolume = stats.disciplines.reduce((sum, item) => sum + item.volumeKg, 0);
  const prs = stats.personalRecords;

  return (
    <div className="progress-page">
      <header className="page-heading">
        <div>
          <p className="eyebrow">Últimas 8 semanas</p>
          <h1>Progreso</h1>
          <p className="muted">Tus mejores marcas y cómo se reparte la carga entre disciplinas.</p>
        </div>
      </header>

      <section className="stats-grid" aria-label="Totales del período">
        <StatCard label="Sesiones" value={formatNumber(stats.totals.sessions)} accent="lime" />
        <StatCard
          label="Volumen total"
          value={formatVolume(stats.totals.volumeKg)}
          hint="Series con peso"
          accent="blue"
        />
        <StatCard label="Tiempo" value={formatMinutes(stats.totals.minutes)} accent="neutral" />
        <StatCard label="PRs" value={formatNumber(stats.totals.prs)} accent="orange" />
      </section>

      <section className="panel" aria-labelledby="progress-chart-title">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Semana a semana</p>
            <h2 id="progress-chart-title">Volumen acumulado</h2>
          </div>
        </div>
        <WeeklyVolumeChart data={stats.weekly} />
      </section>

      <div className="progress-grid">
        <section className="panel" aria-labelledby="pr-title">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Récords personales</p>
              <h2 id="pr-title">PRs</h2>
            </div>
          </div>

          {prs.length === 0 ? (
            <p className="empty-copy">Registrá series con peso o repeticiones para ver tus PRs.</p>
          ) : (
            <div className="pr-grid">
              {prs.map((pr) => (
                <article className="pr-card" key={`${pr.movement}-${pr.achievedOn}`}>
                  <p className="pr-movement">{pr.movement}</p>
                  <p className="pr-value">
                    {pr.bestWeightKg != null
                      ? `${formatWeight(pr.bestWeightKg)} kg`
                      : pr.bestSeconds != null
                        ? `${formatWeight(pr.bestSeconds)} s`
                        : `${formatNumber(pr.bestReps)} reps`}
                  </p>
                  <p className="pr-meta">
                    {pr.bestWeightKg != null && pr.bestReps != null
                      ? `${pr.bestReps} reps · `
                      : pr.bestWeightKg != null && pr.bestSeconds != null
                        ? `${formatWeight(pr.bestSeconds)} s · `
                        : ""}
                    {formatDayMonth(pr.achievedOn)}
                  </p>
                </article>
              ))}
            </div>
          )}
        </section>

        <section className="panel" aria-labelledby="mix-title">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Distribución</p>
              <h2 id="mix-title">Mezcla de disciplinas</h2>
            </div>
          </div>

          {stats.disciplines.length === 0 ? (
            <p className="empty-copy">Todavía no hay datos para comparar.</p>
          ) : (
            <div className="discipline-mix">
              {stats.disciplines.map((item) => {
                const share = totalVolume > 0 ? (item.volumeKg / totalVolume) * 100 : 0;
                return (
                  <div className="mix-row" key={item.discipline}>
                    <div className="mix-label">
                      <span className={`dot dot-${item.discipline.toLowerCase()}`} aria-hidden="true" />
                      <span>{DISCIPLINE_LABELS[item.discipline]}</span>
                      <small>{item.sessions} sesiones</small>
                    </div>
                    <div className="mix-track">
                      <div
                        className={`mix-fill fill-${item.discipline.toLowerCase()}`}
                        style={{ width: `${Math.max(2, share)}%` }}
                      />
                    </div>
                    <span className="mix-value">{formatVolume(item.volumeKg)}</span>
                  </div>
                );
              })}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
