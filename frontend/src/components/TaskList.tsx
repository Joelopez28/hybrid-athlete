import { useState } from "react";
import type { Task, TaskStatus } from "../api/types";
import { TaskItem } from "./TaskItem";

interface Props {
  tasks: Task[];
  onStatusChange: (id: string, status: TaskStatus) => Promise<void>;
  onDelete: (id: string) => Promise<void>;
}

export function TaskList({ tasks, onStatusChange, onDelete }: Props) {
  const [busyId, setBusyId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  async function run(id: string, action: () => Promise<void>) {
    setBusyId(id);
    setActionError(null);
    try {
      await action();
    } catch (cause) {
      setActionError(cause instanceof Error ? cause.message : "No se pudo completar la acción");
    } finally {
      setBusyId(null);
    }
  }

  async function handleDelete(task: Task) {
    if (!window.confirm(`¿Eliminar "${task.title}"? Esta acción no se puede deshacer.`)) {
      return;
    }
    await run(task.id, () => onDelete(task.id));
  }

  return (
    <div className="task-list">
      {actionError && (
        <p className="error-banner" role="alert">
          {actionError}
        </p>
      )}
      <ul className="task-grid">
        {tasks.map((task) => (
          <li key={task.id}>
            <TaskItem
              task={task}
              busy={busyId === task.id}
              onStatusChange={(id, status) => run(id, () => onStatusChange(id, status))}
              onDelete={handleDelete}
            />
          </li>
        ))}
      </ul>
    </div>
  );
}
