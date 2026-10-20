import { useCallback, useEffect, useState } from "react";
import { api } from "../api/client";
import type { AthleteTier, LeaderboardEntry, LeaderboardMetric } from "../api/types";

export function useLeaderboard(
  weeks: number,
  metric: LeaderboardMetric,
  tier: AthleteTier | null,
) {
  const [entries, setEntries] = useState<LeaderboardEntry[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    api
      .leaderboard(weeks, metric, tier, controller.signal)
      .then(setEntries)
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === "AbortError") {
          return;
        }
        setError(cause instanceof Error ? cause.message : "No se pudo cargar el ranking");
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });
    return () => controller.abort();
  }, [weeks, metric, tier, reloadKey]);

  const reload = useCallback(() => setReloadKey((key) => key + 1), []);

  return { entries, loading, error, reload };
}
