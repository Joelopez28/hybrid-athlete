import { useCallback, useEffect, useState } from "react";
import { api } from "../api/client";
import type { Athlete, Exercise, LeaderboardEntry, Stats, TrainingSession } from "../api/types";

interface RukyData {
  me: Athlete;
  exercises: Exercise[];
  sessions: TrainingSession[];
  stats: Stats;
  leaderboard: LeaderboardEntry[];
}

export function useRukyData(enabled: boolean) {
  const [data, setData] = useState<RukyData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    if (!enabled) {
      setLoading(false);
      return;
    }
    const controller = new AbortController();
    setLoading(true);
    setError(null);

    Promise.all([
      api.me(controller.signal),
      api.exercises(controller.signal),
      api.sessions(50, controller.signal),
      api.stats(8, controller.signal),
      api.leaderboard(4, "VOLUME", null, controller.signal),
    ])
      .then(([me, exercises, sessionsPage, stats, leaderboard]) => {
        setData({ me, exercises, sessions: sessionsPage.items, stats, leaderboard });
      })
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === "AbortError") {
          return;
        }
        setError(cause instanceof Error ? cause.message : "No se pudo cargar la app");
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [enabled, reloadKey]);

  const reload = useCallback(() => setReloadKey((key) => key + 1), []);

  return { data, loading, error, reload };
}
