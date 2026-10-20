import { useCallback, useEffect, useState } from "react";
import { api } from "../api/client";
import type { CompetitionDetail } from "../api/types";

export function useCompetition() {
  const [detail, setDetail] = useState<CompetitionDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);

    api
      .competitions(controller.signal)
      .then((competitions) => {
        if (competitions.length === 0) {
          setDetail(null);
          return null;
        }
        return api.competition(competitions[0]!.slug, controller.signal);
      })
      .then((competition) => {
        if (competition) {
          setDetail(competition);
        }
      })
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === "AbortError") {
          return;
        }
        setError(cause instanceof Error ? cause.message : "No se pudo cargar la competencia");
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [reloadKey]);

  const reload = useCallback(() => setReloadKey((key) => key + 1), []);

  return { detail, loading, error, reload };
}
