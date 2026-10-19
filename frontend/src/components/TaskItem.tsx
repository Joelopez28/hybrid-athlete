import type { ChangeEvent } from "react";
import type { Task, TaskStatus } from "../api/types";
import { PRIORITY_LABELS, STATUS_LABELS, TASK_STATUSES, isTaskStatus } from "../api/types";
import { formatDate, formatLocalDate, isOverdue } from "../format";

interface Props {
  task: Task;
  busy: boolean;
  onStatusChange: (id: string, status: TaskStatus) => Promise<void>;
  onDelete: (task: Task) => Promise<void>;
}

export function TaskItem({ task, busy, onStatusChange, onDelete }: Props) {
  const overdue = isOverdue(task.dueDate, task.status);

  function handleStatusChange(event: ChangeEvent<HTMLSelectElement>) {
    const value = event.target.value;
    if (isTaskStatus(value) && value !== task.status) {
      void onStatusChange(task.id, value);
    }
  }

  return (
    <article
      className={`task-card priority-${task.priority.toLowerCase()} ${task.status === "DONE" ? "is-done" : ""}`}
      aria-busy={busy}
    >
      <div className="task-main">
        <h3 className="task-title">{task.title}</h3>
        {task.description && <p className="task-description">{task.description}</p>}
        <div className="task-meta">
          <span className={`badge badge-${task.priority.toLowerCase()}`}>
            {PRIORITY_LABELS[task.priority]}
          </span>
          <span className={overdue ? "due overdue" : "due"}>
            {task.dueDate ? `Vence ${formatDate(task.dueDate)}` : "Sin fecha límite"}
          </span>
          <span className="created">Creada {formatLocalDate(task.createdAt)}</span>
        </div>
      </div>

      <div className="task-actions">
        <label className="sr-only" htmlFor={`status-${task.id}`}>
          Estado de {task.title}
        </label>
        <select
          id={`status-${task.id}`}
          className="status-select"
          value={task.status}
          onChange={handleStatusChange}
          disabled={busy}
        >
          {TASK_STATUSES.map((status) => (
            <option key={status} value={status}>
              {STATUS_LABELS[status]}
            </option>
          ))}
        </select>
        <button
          type="button"
          className="text-button danger"
          onClick={() => void onDelete(task)}
          disabled={busy}
        >
          Eliminar
        </button>
      </div>
    </article>
  );
}
