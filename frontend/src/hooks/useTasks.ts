import { useCallback, useEffect, useState } from "react";
import { api } from "../api/client";
import type { CreateTaskInput, Page, Task, TaskStatus, UpdateTaskInput } from "../api/types";

export const PAGE_SIZE = 8;

export type TaskFilter = TaskStatus | "ALL";

export function useTasks(filter: TaskFilter, page: number) {
  const [data, setData] = useState<Page<Task> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);

    api
      .listTasks({
        status: filter === "ALL" ? undefined : filter,
        page,
        size: PAGE_SIZE,
        signal: controller.signal,
      })
      .then((result) => {
        setData(result);
      })
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === "AbortError") {
          return;
        }
        setError(cause instanceof Error ? cause.message : "No se pudieron cargar las tareas");
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [filter, page, reloadKey]);

  const reload = useCallback(() => {
    setReloadKey((key) => key + 1);
  }, []);

  const create = useCallback(
    async (input: CreateTaskInput) => {
      const task = await api.createTask(input);
      reload();
      return task;
    },
    [reload],
  );

  const update = useCallback(
    async (id: string, patch: UpdateTaskInput) => {
      const task = await api.updateTask(id, patch);
      reload();
      return task;
    },
    [reload],
  );

  const remove = useCallback(
    async (id: string) => {
      await api.deleteTask(id);
      reload();
    },
    [reload],
  );

  return { data, loading, error, reload, create, update, remove };
}
