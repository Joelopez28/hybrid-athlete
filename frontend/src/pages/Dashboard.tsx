import type { Athlete, LeaderboardEntry, Stats, TrainingSession } from "../api/types";
import type { Tab } from "../components/Header";
import { SessionCard } from "../components/SessionCard";
import { StatCard } from "../components/StatCard";
import { WeeklyVolumeChart } from "../components/WeeklyVolumeChart";
import {
  formatMinutes,
  formatNumber,
  formatVolume,
  greeting,
  initials,
} from "../format";

interface Props {
  athlete: Athlete;
  stats: Stats;
  sessions: TrainingSession[];
  leaderboard: LeaderboardEntry[];
  onNavigate: (tab: Tab) => void;
}

export function Dashboard({ athlete, stats, sessions, leaderboard, onNavigate }: Props) {
  const thisWeek = stats.weekly.at(-1);
  const recent = sessions.slice(0, 3);
  const elite = athlete.tier === "ELITE";

  return (
    <div className="dashboard">
      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">{elite ? "Ruky Elite · temporada 01" : "División Open"}</p>
          <h1>
            {greeting()}, {athlete.displayName}.
          </h1>
          <p className="hero-sub">
            Entrená con intención, registrá cada serie y dejá que los números hablen. CrossFit,
            powerlifting, calistenia y HYROX en un solo lugar.
          </p>
          <div className="hero-actions">
            <button type="button" className="primary-button" onClick={() => onNavigate("log")}>
              Registrar sesión
            </button>
            <button type="button" className="secondary-button" onClick={() => onNavigate("progress")}>
              Ver progreso
            </button>
          </div>
        </div>

        <aside className={`elite-card ${elite ? "is-elite" : ""}`}>
          <span className="elite-kicker">{elite ? "División actual" : "Tu división"}</span>
          <strong className="elite-name">{elite ? "RUKY ELITE" : "OPEN"}</strong>
          <p>
            {elite
              ? "Estás dentro de la división de élite. Tus sesiones suman al ranking Ruky Elite."
              : "Competí en la división Open. Los requisitos de acceso a Ruky Elite se definen en la próxima fase."}
          </p>
          <div className="elite-meta">
            <span className="avatar" aria-hidden="true">
              {initials(athlete.displayName)}
            </span>
            <span>
              {athlete.country ?? "—"} · {athlete.bodyweightKg ?? "—"} kg
            </span>
          </div>
        </aside>
      </section>

      <section className="stats-grid" aria-label="Resumen de la semana">
        <StatCard
          label="Sesiones esta semana"
          value={formatNumber(thisWeek?.sessions ?? 0)}
          hint={`${formatMinutes(thisWeek?.minutes ?? 0)} de entrenamiento`}
          accent="lime"
        />
        <StatCard
          label="Volumen semanal"
          value={formatVolume(thisWeek?.volumeKg ?? 0)}
          hint="Solo series con peso externo"
          accent="blue"
        />
        <StatCard
          label="PRs registrados"
          value={formatNumber(stats.totals.prs)}
          hint="Mejor marca por ejercicio"
          accent="orange"
        />
        <StatCard
          label="Historial"
          value={formatNumber(stats.totals.sessions)}
          hint={`${formatMinutes(stats.totals.minutes)} en 8 semanas`}
          accent="neutral"
        />
      </section>

      <div className="dashboard-grid">
        <section className="panel" aria-labelledby="volume-title">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Tendencia</p>
              <h2 id="volume-title">Carga de trabajo</h2>
            </div>
            <button type="button" className="text-button" onClick={() => onNavigate("progress")}>
              Ver progreso
            </button>
          </div>
          <WeeklyVolumeChart data={stats.weekly} />
        </section>

        <section className="panel" aria-labelledby="recent-title">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Últimas</p>
              <h2 id="recent-title">Sesiones recientes</h2>
            </div>
            <button type="button" className="text-button" onClick={() => onNavigate("history")}>
              Ver todo
            </button>
          </div>
          {recent.length === 0 ? (
            <p className="empty-copy">Todavía no registraste sesiones. Empezá hoy.</p>
          ) : (
            <div className="recent-list">
              {recent.map((session) => (
                <SessionCard key={session.id} session={session} compact />
              ))}
            </div>
          )}
        </section>
      </div>

      <div className="dashboard-grid">
        <section className="panel" aria-labelledby="board-title">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Comunidad · 4 semanas</p>
              <h2 id="board-title">Ranking de volumen</h2>
            </div>
            <button type="button" className="text-button" onClick={() => onNavigate("community")}>
              Ver ranking
            </button>
          </div>
          <ol className="mini-board">
            {leaderboard.slice(0, 4).map((entry) => (
              <li key={entry.athleteId} className={entry.me ? "is-me" : ""}>
                <span className="board-rank">{entry.rank}</span>
                <span className="avatar avatar-sm" aria-hidden="true">
                  {initials(entry.displayName)}
                </span>
                <span className="board-name">
                  {entry.displayName}
                  {entry.tier === "ELITE" && <small>Elite</small>}
                </span>
                <span className="board-value">{formatVolume(entry.value)}</span>
              </li>
            ))}
          </ol>
        </section>

        <section className="panel elite-explain" aria-labelledby="elite-title">
          <p className="eyebrow">Próxima fase</p>
          <h2 id="elite-title">Strong Fit · competencias</h2>
          <p>
            Eventos con varias pruebas, puntaje por posición y ranking en vivo. La base de datos ya
            guarda tus resultados; estamos definiendo el formato de la primera competencia.
          </p>
          <ul className="check-list">
            <li>Divisiones por disciplina y nivel</li>
            <li>Pruebas con puntaje y desempates</li>
            <li>Leaderboard por temporada</li>
          </ul>
          <button type="button" className="secondary-button" onClick={() => onNavigate("community")}>
            Ver la comunidad
          </button>
        </section>
      </div>
    </div>
  );
}
