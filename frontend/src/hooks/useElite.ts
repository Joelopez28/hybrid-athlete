import { useCallback, useEffect, useState } from "react";
import { api } from "../api/client";
import type { EliteStatus } from "../api/types";

export function useElite() {
  const [status, setStatus] = useState<EliteStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    api
      .eliteStatus(controller.signal)
      .then(setStatus)
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === "AbortError") {
          return;
        }
        setError(cause instanceof Error ? cause.message : "No se pudo cargar la clasificación");
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });
    return () => controller.abort();
  }, [reloadKey]);

  const reload = useCallback(() => setReloadKey((key) => key + 1), []);

  return { status, loading, error, reload };
}
