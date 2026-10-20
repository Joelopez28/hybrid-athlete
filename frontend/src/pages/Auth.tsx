import { useState, type FormEvent } from "react";
import { api, setToken } from "../api/client";
import type { Discipline, Sex } from "../api/types";
import { DISCIPLINES, DISCIPLINE_LABELS } from "../api/types";

interface Props {
  onAuthenticated: (token: string) => void;
}

type Mode = "login" | "register";

export function Auth({ onAuthenticated }: Props) {
  const [mode, setMode] = useState<Mode>("login");
  const [identifier, setIdentifier] = useState("ruky@ruky.demo");
  const [password, setPassword] = useState("ruky1234");
  const [displayName, setDisplayName] = useState("");
  const [handle, setHandle] = useState("");
  const [email, setEmail] = useState("");
  const [registerPassword, setRegisterPassword] = useState("");
  const [sex, setSex] = useState<Sex>("OTHER");
  const [bodyweight, setBodyweight] = useState("");
  const [country, setCountry] = useState("CL");
  const [disciplines, setDisciplines] = useState<Discipline[]>(["CROSSFIT"]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function changeMode(next: Mode) {
    setMode(next);
    setError(null);
  }

  function toggleDiscipline(discipline: Discipline) {
    setDisciplines((current) =>
      current.includes(discipline)
        ? current.filter((item) => item !== discipline)
        : [...current, discipline],
    );
  }

  async function handleLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const response = await api.login({ identifier: identifier.trim(), password });
      setToken(response.token);
      onAuthenticated(response.token);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "No se pudo iniciar sesión");
    } finally {
      setBusy(false);
    }
  }

  async function handleRegister(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const response = await api.registerAthlete({
        displayName: displayName.trim(),
        handle: handle.trim().toLowerCase(),
        email: email.trim().toLowerCase(),
        password: registerPassword,
        sex,
        bodyweightKg: bodyweight ? Number.parseFloat(bodyweight) : null,
        country: country.trim() || null,
        disciplines,
      });
      setToken(response.token);
      onAuthenticated(response.token);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "No se pudo crear la cuenta");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-shell">
        <aside className="auth-brand">
          <div className="auth-logo">
            <span className="brand-mark" aria-hidden="true">
              R
            </span>
            <span className="brand-name">RUKY</span>
          </div>
          <p className="eyebrow">Training club</p>
          <h1>Entrená. Registrá. Competí.</h1>
          <p>
            CrossFit, powerlifting, calistenia y HYROX en un solo lugar. Tus sesiones, tus PRs y
            tus competencias, con una comunidad que empuja.
          </p>
          <ul className="auth-points">
            <li>Registro de sesiones y progreso real</li>
            <li>Ranking de comunidad y división Ruky Elite</li>
            <li>Competencias Strong Fit con puntaje por posición</li>
          </ul>
          <p className="auth-demo">
            Demo: <strong>ruky@ruky.demo</strong> · <strong>ruky1234</strong>
          </p>
        </aside>

        <section className="auth-card" aria-labelledby="auth-title">
          <div className="auth-tabs" role="tablist" aria-label="Acceso">
            <button
              type="button"
              role="tab"
              aria-selected={mode === "login"}
              onClick={() => changeMode("login")}
            >
              Iniciar sesión
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={mode === "register"}
              onClick={() => changeMode("register")}
            >
              Crear cuenta
            </button>
          </div>

          <h2 id="auth-title">{mode === "login" ? "Volver al entrenamiento" : "Crear tu perfil"}</h2>
          <p className="muted">
            {mode === "login"
              ? "Entrá para ver tu historial, tu progreso y tus competencias."
              : "Tu cuenta arranca en la división Open. Después podés competir en RX o Ruky Elite."}
          </p>

          {mode === "login" ? (
            <form className="auth-form" onSubmit={handleLogin}>
              <label className="field">
                <span>Email o usuario</span>
                <input
                  type="text"
                  autoComplete="username"
                  value={identifier}
                  onChange={(event) => setIdentifier(event.target.value)}
                  required
                />
              </label>
              <label className="field">
                <span>Contraseña</span>
                <input
                  type="password"
                  autoComplete="current-password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  required
                />
              </label>
              {error && (
                <p className="form-message error" role="alert">
                  {error}
                </p>
              )}
              <button type="submit" className="primary-button" disabled={busy}>
                {busy ? "Entrando…" : "Entrar"}
              </button>
            </form>
          ) : (
            <form className="auth-form" onSubmit={handleRegister}>
              <div className="auth-grid">
                <label className="field">
                  <span>Nombre</span>
                  <input
                    type="text"
                    value={displayName}
                    maxLength={80}
                    onChange={(event) => setDisplayName(event.target.value)}
                    required
                  />
                </label>
                <label className="field">
                  <span>Usuario</span>
                  <input
                    type="text"
                    value={handle}
                    pattern="[a-z0-9_]{3,30}"
                    placeholder="minúsculas, números o _"
                    onChange={(event) => setHandle(event.target.value.toLowerCase())}
                    required
                  />
                </label>
                <label className="field">
                  <span>Email</span>
                  <input
                    type="email"
                    autoComplete="email"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    required
                  />
                </label>
                <label className="field">
                  <span>Contraseña</span>
                  <input
                    type="password"
                    autoComplete="new-password"
                    minLength={8}
                    maxLength={72}
                    value={registerPassword}
                    onChange={(event) => setRegisterPassword(event.target.value)}
                    required
                  />
                </label>
                <label className="field">
                  <span>Sexo</span>
                  <select value={sex} onChange={(event) => setSex(event.target.value as Sex)}>
                    <option value="OTHER">Prefiero no decir</option>
                    <option value="FEMALE">Femenino</option>
                    <option value="MALE">Masculino</option>
                  </select>
                </label>
                <label className="field">
                  <span>Peso corporal (kg)</span>
                  <input
                    type="number"
                    inputMode="decimal"
                    min={30}
                    max={250}
                    step={0.1}
                    value={bodyweight}
                    onChange={(event) => setBodyweight(event.target.value)}
                  />
                </label>
                <label className="field field-wide">
                  <span>País</span>
                  <input
                    type="text"
                    value={country}
                    maxLength={80}
                    onChange={(event) => setCountry(event.target.value)}
                  />
                </label>
              </div>

              <fieldset className="auth-disciplines">
                <legend>Disciplinas</legend>
                <div className="chip-row">
                  {DISCIPLINES.map((discipline) => (
                    <button
                      key={discipline}
                      type="button"
                      className={`choice-chip choice-${discipline.toLowerCase()}`}
                      aria-pressed={disciplines.includes(discipline)}
                      onClick={() => toggleDiscipline(discipline)}
                    >
                      {DISCIPLINE_LABELS[discipline]}
                    </button>
                  ))}
                </div>
              </fieldset>

              {error && (
                <p className="form-message error" role="alert">
                  {error}
                </p>
              )}
              <button type="submit" className="primary-button" disabled={busy}>
                {busy ? "Creando cuenta…" : "Crear cuenta"}
              </button>
            </form>
          )}
        </section>
      </div>
    </div>
  );
}
