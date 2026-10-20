import type { Athlete } from "../api/types";
import { initials } from "../format";

export type Tab = "dashboard" | "log" | "history" | "progress" | "community";

interface Props {
  tab: Tab;
  athlete: Athlete;
  onNavigate: (tab: Tab) => void;
}

const NAV: { id: Tab; label: string }[] = [
  { id: "dashboard", label: "Hoy" },
  { id: "log", label: "Registrar" },
  { id: "history", label: "Historial" },
  { id: "progress", label: "Progreso" },
  { id: "community", label: "Comunidad" },
];

export function Header({ tab, athlete, onNavigate }: Props) {
  return (
    <header className="site-header">
      <div className="header-inner">
        <button
          type="button"
          className="brand-button"
          onClick={() => onNavigate("dashboard")}
          aria-label="RUKY, ir al inicio"
        >
          <span className="brand-mark" aria-hidden="true">
            R
          </span>
          <span className="brand-copy">
            <span className="brand-name">RUKY</span>
            <span className="brand-sub">Training club</span>
          </span>
        </button>

        <nav className="main-nav" aria-label="Secciones principales">
          {NAV.map((item) => (
            <button
              key={item.id}
              type="button"
              className="nav-item"
              aria-current={tab === item.id ? "page" : undefined}
              onClick={() => onNavigate(item.id)}
            >
              {item.label}
            </button>
          ))}
        </nav>

        <div className="athlete-chip">
          <span className="avatar avatar-sm" aria-hidden="true">
            {initials(athlete.displayName)}
          </span>
          <span className="athlete-chip-copy">
            <strong>{athlete.displayName}</strong>
            <span className={`tier-badge tier-${athlete.tier.toLowerCase()}`}>
              {athlete.tier === "ELITE" ? "Ruky Elite" : "Open"}
            </span>
          </span>
        </div>
      </div>
    </header>
  );
}
