import { useState, type FormEvent } from "react";
import type {
  CreateSessionInput,
  Discipline,
  Exercise,
  SessionType,
  WodDivision,
  WodFormat,
} from "../api/types";
import {
  DISCIPLINES,
  DISCIPLINE_LABELS,
  DIVISION_LABELS,
  SESSION_TYPE_LABELS,
  SESSION_TYPES,
  WOD_DIVISIONS,
  WOD_FORMAT_LABELS,
  WOD_FORMATS,
  isSessionType,
  isWodDivision,
  isWodFormat,
} from "../api/types";
import { todayIso } from "../format";

interface Props {
  exercises: Exercise[];
  onCreate: (input: CreateSessionInput) => Promise<unknown>;
  onSaved: () => void;
}

interface DraftEntry {
  key: string;
  exerciseId: string;
  reps: string;
  weight: string;
  seconds: string;
}

let entrySequence = 0;

function newEntry(exerciseId: string): DraftEntry {
  entrySequence += 1;
  return { key: `entry-${entrySequence}`, exerciseId, reps: "", weight: "", seconds: "" };
}

function defaultTypeFor(discipline: Discipline): SessionType {
  switch (discipline) {
    case "POWERLIFTING":
      return "STRENGTH";
    case "CALISTHENICS":
      return "SKILL";
    case "HYROX":
      return "HYROX_SIM";
    default:
      return "WOD";
  }
}

function needsEntries(type: SessionType): boolean {
  return type === "STRENGTH" || type === "SKILL" || type === "MIXED";
}

function parseInteger(value: string): number | null {
  if (!value.trim()) {
    return null;
  }
  const parsed = Number.parseInt(value, 10);
  return Number.isFinite(parsed) ? parsed : null;
}

function parseDecimal(value: string): number | null {
  if (!value.trim()) {
    return null;
  }
  const parsed = Number.parseFloat(value.replace(",", "."));
  return Number.isFinite(parsed) ? parsed : null;
}

export function SessionForm({ exercises, onCreate, onSaved }: Props) {
  const [discipline, setDiscipline] = useState<Discipline>("CROSSFIT");
  const [type, setType] = useState<SessionType>("WOD");
  const [date, setDate] = useState(todayIso());
  const [title, setTitle] = useState("");
  const [notes, setNotes] = useState("");
  const [duration, setDuration] = useState("60");
  const [rpe, setRpe] = useState("8");
  const [wodFormat, setWodFormat] = useState<WodFormat>("FOR_TIME");
  const [wodScore, setWodScore] = useState("");
  const [wodDivision, setWodDivision] = useState<WodDivision>("RX");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [entries, setEntries] = useState<DraftEntry[]>([]);

  const availableExercises = exercises.filter((exercise) => exercise.discipline === discipline);
  const firstExercise = availableExercises[0]?.id ?? "";
  const showEntries = needsEntries(type);
  const showResult = type === "WOD" || type === "HYROX_SIM";

  function changeDiscipline(next: Discipline) {
    const nextType = defaultTypeFor(next);
    const first = exercises.find((exercise) => exercise.discipline === next)?.id ?? "";
    setDiscipline(next);
    setType(nextType);
    setEntries(needsEntries(nextType) ? [newEntry(first)] : []);
    setWodDivision(next === "HYROX" ? "OPEN" : "RX");
    setSuccess(null);
  }

  function changeType(next: SessionType) {
    setType(next);
    if (needsEntries(next) && entries.length === 0) {
      setEntries([newEntry(firstExercise)]);
    }
    setSuccess(null);
  }

  function updateEntry(key: string, patch: Partial<DraftEntry>) {
    setEntries((current) =>
      current.map((entry) => (entry.key === key ? { ...entry, ...patch } : entry)),
    );
  }

  function addEntry() {
    const previous = entries.at(-1);
    setEntries((current) => [...current, newEntry(previous?.exerciseId ?? firstExercise)]);
  }

  function removeEntry(key: string) {
    setEntries((current) => current.filter((entry) => entry.key !== key));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const cleanTitle = title.trim();
    if (!cleanTitle) {
      setError("Poné un título a la sesión");
      return;
    }

    const filledEntries = entries.filter(
      (entry) => entry.exerciseId && (entry.reps.trim() || entry.seconds.trim()),
    );

    if (showEntries && filledEntries.length === 0) {
      setError("Agregá al menos una serie con repeticiones o segundos");
      return;
    }
    if (showResult && !wodScore.trim()) {
      setError(type === "WOD" ? "Un WOD necesita formato y resultado" : "Poné el tiempo total de la simulación");
      return;
    }

    const payload: CreateSessionInput = {
      performedOn: date,
      discipline,
      sessionType: type,
      title: cleanTitle,
      notes: notes.trim() || null,
      durationMin: parseInteger(duration),
      rpe: parseDecimal(rpe),
      wodFormat: type === "WOD" ? wodFormat : null,
      wodScore: showResult ? wodScore.trim() : null,
      wodDivision: showResult ? wodDivision : null,
      entries: showEntries
        ? filledEntries.map((entry, index) => ({
            exerciseId: entry.exerciseId,
            movement: null,
            setIndex: index + 1,
            reps: parseInteger(entry.reps),
            weightKg: parseDecimal(entry.weight),
            seconds: parseDecimal(entry.seconds),
            rpe: null,
          }))
        : [],
    };

    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      await onCreate(payload);
      setSuccess("Sesión guardada. Ya suma para tu progreso y el ranking.");
      setTitle("");
      setNotes("");
      setWodScore("");
      setEntries(showEntries ? [newEntry(firstExercise)] : []);
      onSaved();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "No se pudo guardar la sesión");
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="form-card" onSubmit={handleSubmit} noValidate>
      <header className="form-header">
        <div>
          <p className="eyebrow">Nueva entrada</p>
          <h2>Registrar sesión</h2>
        </div>
        <p className="muted">Cargá lo que hiciste. El resto lo calcula RUKY.</p>
      </header>

      <fieldset className="form-block">
        <legend>Disciplina</legend>
        <div className="chip-row">
          {DISCIPLINES.map((value) => (
            <button
              key={value}
              type="button"
              className={`choice-chip choice-${value.toLowerCase()}`}
              aria-pressed={discipline === value}
              onClick={() => changeDiscipline(value)}
            >
              {DISCIPLINE_LABELS[value]}
            </button>
          ))}
        </div>
      </fieldset>

      <div className="form-grid">
        <div className="field">
          <label htmlFor="session-date">Fecha</label>
          <input
            id="session-date"
            type="date"
            value={date}
            max={todayIso()}
            onChange={(event) => setDate(event.target.value)}
            required
          />
        </div>

        <div className="field">
          <label htmlFor="session-type">Tipo</label>
          <select
            id="session-type"
            value={type}
            onChange={(event) => {
              const value = event.target.value;
              if (isSessionType(value)) {
                changeType(value);
              }
            }}
          >
            {SESSION_TYPES.map((value) => (
              <option key={value} value={value}>
                {SESSION_TYPE_LABELS[value]}
              </option>
            ))}
          </select>
        </div>

        <div className="field field-wide">
          <label htmlFor="session-title">Título</label>
          <input
            id="session-title"
            type="text"
            value={title}
            maxLength={120}
            placeholder="Ej. Fuerza · sentadilla pesada"
            onChange={(event) => setTitle(event.target.value)}
            required
          />
        </div>

        <div className="field">
          <label htmlFor="session-duration">Duración (min)</label>
          <input
            id="session-duration"
            type="number"
            inputMode="numeric"
            min={1}
            max={600}
            value={duration}
            onChange={(event) => setDuration(event.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="session-rpe">RPE (1–10)</label>
          <input
            id="session-rpe"
            type="number"
            inputMode="decimal"
            min={1}
            max={10}
            step={0.5}
            value={rpe}
            onChange={(event) => setRpe(event.target.value)}
          />
        </div>

        <div className="field field-wide">
          <label htmlFor="session-notes">Notas</label>
          <textarea
            id="session-notes"
            rows={2}
            maxLength={2000}
            value={notes}
            placeholder="Sensaciones, cargas, objetivos de la próxima sesión…"
            onChange={(event) => setNotes(event.target.value)}
          />
        </div>
      </div>

      {showResult && (
        <fieldset className="form-block result-block">
          <legend>Resultado</legend>
          <div className="form-grid">
            {type === "WOD" && (
              <div className="field">
                <label htmlFor="wod-format">Formato</label>
                <select
                  id="wod-format"
                  value={wodFormat}
                  onChange={(event) => {
                    const value = event.target.value;
                    if (isWodFormat(value)) {
                      setWodFormat(value);
                    }
                  }}
                >
                  {WOD_FORMATS.map((value) => (
                    <option key={value} value={value}>
                      {WOD_FORMAT_LABELS[value]}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div className="field">
              <label htmlFor="wod-division">División</label>
              <select
                id="wod-division"
                value={wodDivision}
                onChange={(event) => {
                  const value = event.target.value;
                  if (isWodDivision(value)) {
                    setWodDivision(value);
                  }
                }}
              >
                {WOD_DIVISIONS.map((value) => (
                  <option key={value} value={value}>
                    {DIVISION_LABELS[value]}
                  </option>
                ))}
              </select>
            </div>

            <div className="field field-wide">
              <label htmlFor="wod-score">
                {type === "WOD" ? "Score (tiempo, rondas+reps)" : "Tiempo total"}
              </label>
              <input
                id="wod-score"
                type="text"
                maxLength={40}
                value={wodScore}
                placeholder={type === "WOD" ? "Ej. 04:20 u 8+12" : "Ej. 1:18:40"}
                onChange={(event) => setWodScore(event.target.value)}
              />
            </div>
          </div>
        </fieldset>
      )}

      {showEntries && (
        <fieldset className="form-block entries-block">
          <legend>Series</legend>
          <div className="entries-head">
            <p className="muted">Reps o segundos: al menos uno por serie.</p>
            <button type="button" className="secondary-button" onClick={addEntry}>
              + Serie
            </button>
          </div>

          <div className="entries-table">
            <div className="entries-labels" aria-hidden="true">
              <span>#</span>
              <span>Ejercicio</span>
              <span>Reps</span>
              <span>kg</span>
              <span>Seg</span>
              <span />
            </div>

            {entries.map((entry, index) => (
              <div className="entry-row" key={entry.key}>
                <span className="set-index">{index + 1}</span>
                <label className="sr-only" htmlFor={`exercise-${entry.key}`}>
                  Ejercicio de la serie {index + 1}
                </label>
                <select
                  id={`exercise-${entry.key}`}
                  value={entry.exerciseId}
                  onChange={(event) => updateEntry(entry.key, { exerciseId: event.target.value })}
                >
                  {availableExercises.map((exercise) => (
                    <option key={exercise.id} value={exercise.id}>
                      {exercise.name}
                    </option>
                  ))}
                </select>
                <input
                  type="number"
                  inputMode="numeric"
                  min={1}
                  max={1000}
                  placeholder="Reps"
                  aria-label={`Repeticiones de la serie ${index + 1}`}
                  value={entry.reps}
                  onChange={(event) => updateEntry(entry.key, { reps: event.target.value })}
                />
                <input
                  type="number"
                  inputMode="decimal"
                  min={0}
                  max={1000}
                  step={0.5}
                  placeholder="kg"
                  aria-label={`Peso en kg de la serie ${index + 1}`}
                  value={entry.weight}
                  onChange={(event) => updateEntry(entry.key, { weight: event.target.value })}
                />
                <input
                  type="number"
                  inputMode="decimal"
                  min={0}
                  max={7200}
                  placeholder="Seg"
                  aria-label={`Segundos de la serie ${index + 1}`}
                  value={entry.seconds}
                  onChange={(event) => updateEntry(entry.key, { seconds: event.target.value })}
                />
                <button
                  type="button"
                  className="icon-button"
                  aria-label={`Quitar serie ${index + 1}`}
                  onClick={() => removeEntry(entry.key)}
                  disabled={entries.length === 1}
                >
                  ×
                </button>
              </div>
            ))}
          </div>
        </fieldset>
      )}

      {error && (
        <p className="form-message error" role="alert">
          {error}
        </p>
      )}
      {success && (
        <p className="form-message success" role="status">
          {success}
        </p>
      )}

      <button type="submit" className="primary-button" disabled={saving}>
        {saving ? "Guardando…" : "Guardar sesión"}
      </button>
    </form>
  );
}
