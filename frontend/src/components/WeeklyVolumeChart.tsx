import type { WeeklyPoint } from "../api/types";
import { formatNumber, formatVolume, formatWeek } from "../format";

interface Props {
  data: WeeklyPoint[];
  compact?: boolean;
}

export function WeeklyVolumeChart({ data, compact = false }: Props) {
  const max = Math.max(...data.map((point) => point.volumeKg), 1);
  const total = data.reduce((sum, point) => sum + point.volumeKg, 0);
  const hasData = total > 0;

  return (
    <figure className={`volume-chart ${compact ? "compact" : ""}`}>
      <figcaption className="chart-heading">
        <span>Volumen por semana</span>
        <span className="chart-total">{formatVolume(total)}</span>
      </figcaption>

      {hasData ? (
        <div className="chart-bars">
          {data.map((point) => {
            const height = Math.max(3, (point.volumeKg / max) * 100);
            return (
              <div className="chart-col" key={point.weekStart}>
                <div className="chart-track">
                  <div
                    className="chart-bar"
                    style={{ height: `${height}%` }}
                    title={`${formatWeek(point.weekStart)}: ${formatVolume(point.volumeKg)}`}
                  />
                </div>
                <span className="chart-label">{formatWeek(point.weekStart)}</span>
                {!compact && (
                  <span className="chart-value">{formatNumber(point.volumeKg / 1000)}k</span>
                )}
              </div>
            );
          })}
        </div>
      ) : (
        <p className="empty-chart">Todavía no hay volumen registrado en este período.</p>
      )}
    </figure>
  );
}
