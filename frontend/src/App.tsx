import { useEffect, useState } from "react";
import { api } from "./api/client";
import type { CreateSessionInput } from "./api/types";
import { Header, type Tab } from "./components/Header";
import { SessionForm } from "./components/SessionForm";
import { useRukyData } from "./hooks/useRukyData";
import { Community } from "./pages/Community";
import { Dashboard } from "./pages/Dashboard";
import { History } from "./pages/History";
import { Progress } from "./pages/Progress";

function App() {
  const { data, loading, error, reload } = useRukyData();
  const [tab, setTab] = useState<Tab>("dashboard");

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "smooth" });
  }, [tab]);

  async function handleCreate(input: CreateSessionInput) {
    await api.createSession(input);
  }

  async function handleDelete(id: string) {
    await api.deleteSession(id);
    reload();
  }

  if (loading && !data) {
    return (
      <div className="splash">
        <span className="brand-mark" aria-hidden="true">
          R
        </span>
        <p className="brand-name">RUKY</p>
        <p className="muted">Cargando tu entrenamiento…</p>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="splash">
        <span className="brand-mark" aria-hidden="true">
          R
        </span>
        <p className="brand-name">RUKY</p>
        <p className="splash-error">{error ?? "No se pudo cargar la app."}</p>
        <button type="button" className="primary-button" onClick={reload}>
          Reintentar
        </button>
      </div>
    );
  }

  return (
    <div className="app">
      <Header tab={tab} athlete={data.me} onNavigate={setTab} />

      {error && (
        <div className="inline-alert global-alert" role="alert">
          <p>No se pudieron refrescar los datos: {error}</p>
          <button type="button" className="secondary-button" onClick={reload}>
            Reintentar
          </button>
        </div>
      )}

      <main className="page">
        {tab === "dashboard" && (
          <Dashboard
            athlete={data.me}
            stats={data.stats}
            sessions={data.sessions}
            leaderboard={data.leaderboard}
            onNavigate={setTab}
          />
        )}

        {tab === "log" && (
          <div className="log-page">
            <header className="page-heading">
              <div>
                <p className="eyebrow">Carga</p>
                <h1>Registrar</h1>
                <p className="muted">
                  Una sesión, una entrada. Después RUKY calcula volumen, PRs y ranking.
                </p>
              </div>
            </header>
            <SessionForm exercises={data.exercises} onCreate={handleCreate} onSaved={reload} />
          </div>
        )}

        {tab === "history" && (
          <History sessions={data.sessions} onDelete={handleDelete} onNavigate={setTab} />
        )}

        {tab === "progress" && <Progress stats={data.stats} />}

        {tab === "community" && <Community athlete={data.me} />}
      </main>

      <footer className="site-footer">
        <span>RUKY · training club</span>
        <span>Demo local · sin auth todavía · datos en PostgreSQL</span>
      </footer>
    </div>
  );
}

export default App;
