import { useState, type FormEvent } from "react";
import { api } from "../api/client";
import type {
  Athlete,
  CompetitionDivisionInfo,
  CompetitionEventInfo,
  CompetitionStatus,
} from "../api/types";
import {
  COMPETITION_STATUS_LABELS,
  SCORE_TYPE_LABELS,
  SCORE_UNIT_LABELS,
} from "../api/types";
import { useCompetition } from "../hooks/useCompetition";
import { formatDayMonth, initials } from "../format";

interface Props {
  athlete: Athlete;
}

function directionLabel(event: CompetitionEventInfo): string {
  return event.scoreDirection === "ASC" ? "menor es mejor" : "mayor es mejor";
}

function statusClass(status: CompetitionStatus): string {
  return `status-${status.toLowerCase()}`;
}

export function Compete({ athlete }: Props) {
  const { detail, loading, error, reload } = useCompetition();
  const [divisionId, setDivisionId] = useState("");
  const [scores, setScores] = useState<Record<string, string>>({});
  const [boardId, setBoardId] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const registration = detail?.myRegistration ?? null;
  const live = detail?.competition.status === "LIVE";
  const myResults = new Map((detail?.myResults ?? []).map((result) => [result.eventId, result]));
  const defaultBoard =
    detail?.leaderboards.find((board) => board.divisionId === registration?.divisionId) ??
    detail?.leaderboards[0];
  const activeBoard =
    detail?.leaderboards.find((board) => board.divisionId === boardId) ?? defaultBoard;

  async function run(key: string, action: () => Promise<void>, message: string) {
    setBusy(key);
    setActionError(null);
    setNotice(null);
    try {
      await action();
      setNotice(message);
      reload();
    } catch (cause) {
      setActionError(cause instanceof Error ? cause.message : "No se pudo completar la acción");
    } finally {
      setBusy(null);
    }
  }

  async function handleRegister() {
    if (!detail) {
      return;
    }
    if (!divisionId) {
      setActionError("Elegí una división para inscribirte");
      return;
    }
    await run("register", () => api.registerForCompetition(detail.competition.slug, divisionId),
      "Inscripción confirmada. Ya podés cargar tus resultados.");
  }

  async function handleWithdraw() {
    if (!detail) {
      return;
    }
    if (!window.confirm("¿Darte de baja de la competencia? Tus resultados quedan guardados.")) {
      return;
    }
    await run("withdraw", () => api.withdrawFromCompetition(detail.competition.slug),
      "Te dimos de baja de la competencia.");
  }

  async function handleSubmitResult(event: FormEvent<HTMLFormElement>, competitionEvent: CompetitionEventInfo) {
    event.preventDefault();
    if (!detail) {
      return;
    }
    const raw = Number.parseFloat((scores[competitionEvent.id] ?? "").replace(",", "."));
    if (!Number.isFinite(raw) || raw < 0) {
      setActionError("Ingresá un resultado válido");
      return;
    }
    await run(`result-${competitionEvent.id}`,
      () => api.submitCompetitionResult(detail.competition.slug, competitionEvent.id, { rawScore: raw }),
      `Resultado guardado en ${competitionEvent.name}.`);
    setScores((current) => ({ ...current, [competitionEvent.id]: "" }));
  }

  if (loading && !detail) {
    return (
      <div className="compete-page">
        <div className="page-heading">
          <div>
            <p className="eyebrow">Strong Fit</p>
            <h1>Competir</h1>
          </div>
        </div>
        <div className="board-skeleton" role="status" aria-label="Cargando competencia">
          <span />
          <span />
          <span />
        </div>
      </div>
    );
  }

  if (error && !detail) {
    return (
      <div className="compete-page">
        <div className="inline-alert" role="alert">
          <p>{error}</p>
          <button type="button" className="secondary-button" onClick={reload}>
            Reintentar
          </button>
        </div>
      </div>
    );
  }

  if (!detail) {
    return (
      <div className="compete-page">
        <div className="empty-panel">
          <p className="eyebrow">Strong Fit</p>
          <h1>Competir</h1>
          <p>Todavía no hay competencias publicadas. Volvé pronto.</p>
        </div>
      </div>
    );
  }

  const competition = detail.competition;

  return (
    <div className="compete-page">
      <header className="page-heading">
        <div>
          <p className="eyebrow">Strong Fit</p>
          <h1>Competir</h1>
          <p className="muted">Inscribite, cargá tus marcas y seguí el leaderboard en vivo.</p>
        </div>
      </header>

      {actionError && (
        <div className="inline-alert" role="alert">
          <p>{actionError}</p>
          <button type="button" className="secondary-button" onClick={() => setActionError(null)}>
            Cerrar
          </button>
        </div>
      )}
      {notice && (
        <p className="form-message success" role="status">
          {notice}
        </p>
      )}

      <section className="competition-hero panel">
        <div className="competition-copy">
          <span className={`competition-status ${statusClass(competition.status)}`}>
            {COMPETITION_STATUS_LABELS[competition.status]}
          </span>
          <h2>{competition.name}</h2>
          <p>{competition.description}</p>
          <div className="competition-meta">
            {competition.startsOn && (
              <span>
                {formatDayMonth(competition.startsOn)}
                {competition.endsOn ? ` → ${formatDayMonth(competition.endsOn)}` : ""}
              </span>
            )}
            {competition.location && <span>{competition.location}</span>}
            <span>{detail.events.length} pruebas</span>
            <span>{detail.divisions.length} divisiones</span>
          </div>
        </div>

        {registration ? (
          <div className="registration-box is-registered">
            <p className="eyebrow">Mi participación</p>
            <strong>{registration.divisionName}</strong>
            <span className="registration-status">Inscripto</span>
            <p className="muted small">
              Tus resultados cuentan para el leaderboard de esta división.
            </p>
            <button
              type="button"
              className="secondary-button"
              onClick={handleWithdraw}
              disabled={!live || busy === "withdraw"}
            >
              {busy === "withdraw" ? "Saliendo…" : "Darme de baja"}
            </button>
          </div>
        ) : (
          <div className="registration-box">
            <p className="eyebrow">Inscripción</p>
            <label className="field">
              <span>División</span>
              <select
                value={divisionId}
                onChange={(event) => setDivisionId(event.target.value)}
                disabled={!live}
              >
                <option value="">Elegí una división</option>
                {detail.divisions.map((division: CompetitionDivisionInfo) => (
                  <option
                    key={division.id}
                    value={division.id}
                    disabled={division.tier === "ELITE" && athlete.tier !== "ELITE"}
                  >
                    {division.name} · {division.participants}
                    {division.capacity ? `/${division.capacity}` : ""}
                  </option>
                ))}
              </select>
            </label>
            <button
              type="button"
              className="primary-button"
              onClick={handleRegister}
              disabled={!live || !divisionId || busy === "register"}
            >
              {busy === "register" ? "Inscribiendo…" : "Inscribirme"}
            </button>
            <p className="muted small">
              {athlete.tier === "ELITE"
                ? "Como atleta Elite podés competir en Ruky Elite o RX."
                : "La división Ruky Elite es solo para atletas Elite."}
            </p>
          </div>
        )}
      </section>

      {!live && (
        <p className="inline-alert" role="status">
          La competencia no está abierta para inscripciones ni resultados.
        </p>
      )}

      <div className="competition-layout">
        <section aria-labelledby="events-title">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Programa</p>
              <h2 id="events-title">Pruebas</h2>
            </div>
          </div>

          <div className="events-grid">
            {detail.events.map((competitionEvent) => {
              const myResult = myResults.get(competitionEvent.id);
              return (
                <article className="event-card" key={competitionEvent.id}>
                  <div className="event-head">
                    <span className="event-index">
                      {String(competitionEvent.orderIndex).padStart(2, "0")}
                    </span>
                    <div>
                      <h3>{competitionEvent.name}</h3>
                      <p>{competitionEvent.description}</p>
                    </div>
                    <span className="event-rule">
                      {SCORE_TYPE_LABELS[competitionEvent.scoreType]} ·{" "}
                      {directionLabel(competitionEvent)}
                    </span>
                  </div>

                  {myResult && (
                    <div className="event-result">
                      <span>Tu marca</span>
                      <strong>{myResult.displayScore}</strong>
                      <small>
                        {myResult.place ? `${myResult.place}º lugar` : "Sin puntos"} ·{" "}
                        {myResult.points} pts
                      </small>
                    </div>
                  )}

                  {registration && live && (
                    <form
                      className="event-form"
                      onSubmit={(event) => handleSubmitResult(event, competitionEvent)}
                    >
                      <label>
                        Resultado ({SCORE_UNIT_LABELS[competitionEvent.scoreType]})
                        <input
                          type="number"
                          inputMode="decimal"
                          min={0}
                          step="0.01"
                          value={scores[competitionEvent.id] ?? ""}
                          placeholder={myResult ? "Nuevo resultado" : "Tu resultado"}
                          onChange={(event) =>
                            setScores((current) => ({
                              ...current,
                              [competitionEvent.id]: event.target.value,
                            }))
                          }
                        />
                      </label>
                      <button
                        type="submit"
                        className="secondary-button"
                        disabled={busy === `result-${competitionEvent.id}`}
                      >
                        {busy === `result-${competitionEvent.id}` ? "Guardando…" : "Guardar"}
                      </button>
                    </form>
                  )}

                  {!registration && (
                    <p className="event-hint">Inscribite para cargar tu resultado.</p>
                  )}
                </article>
              );
            })}
          </div>
        </section>

        <section className="panel leaderboard-panel" aria-labelledby="competition-board-title">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Puntos por posición</p>
              <h2 id="competition-board-title">Leaderboard</h2>
            </div>
          </div>

          <div className="board-tabs" role="group" aria-label="Divisiones">
            {detail.leaderboards.map((board) => (
              <button
                key={board.divisionId}
                type="button"
                className="choice-chip"
                aria-pressed={activeBoard?.divisionId === board.divisionId}
                onClick={() => setBoardId(board.divisionId)}
              >
                {board.divisionName} · {board.rows.length}
              </button>
            ))}
          </div>

          {activeBoard && activeBoard.rows.length === 0 ? (
            <p className="empty-copy">Todavía no hay resultados en esta división.</p>
          ) : (
            activeBoard && (
              <div className="table-scroll">
                <table className="competition-table">
                  <thead>
                    <tr>
                      <th scope="col">#</th>
                      <th scope="col">Atleta</th>
                      {detail.events.map((competitionEvent) => (
                        <th scope="col" key={competitionEvent.id} title={competitionEvent.name}>
                          {competitionEvent.name}
                        </th>
                      ))}
                      <th scope="col">Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {activeBoard.rows.map((row) => (
                      <tr key={row.athleteId} className={row.athleteId === athlete.id ? "is-me" : ""}>
                        <td className="cell-rank">{row.rank}</td>
                        <td>
                          <span className="table-athlete">
                            <span className="avatar avatar-sm" aria-hidden="true">
                              {initials(row.displayName)}
                            </span>
                            <span>
                              <strong>{row.displayName}</strong>
                              <small>
                                @{row.handle}
                                {row.tier === "ELITE" ? " · Elite" : ""}
                              </small>
                            </span>
                          </span>
                        </td>
                        {row.eventScores.map((score) => (
                          <td key={score.eventId} className="cell-points">
                            {score.points > 0 ? (
                              <>
                                <strong>{score.points}</strong>
                                <small>{score.place}º</small>
                              </>
                            ) : (
                              <span className="muted">—</span>
                            )}
                          </td>
                        ))}
                        <td className="cell-total">{row.totalPoints}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          )}

          <p className="table-hint">Deslizá la tabla para ver todas las pruebas. La columna Total queda fija.</p>

          <p className="scoring-note">
            Cada prueba reparte puntos por posición: 1º recibe la mayor cantidad y los demás bajan
            de a 10. Los resultados ausentes suman 0. Gana quien más puntos acumula; los empates se
            definen por cantidad de primeros puestos.
          </p>
        </section>
      </div>
    </div>
  );
}
