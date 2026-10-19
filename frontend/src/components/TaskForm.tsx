import { useRef, useState, type FormEvent } from "react";
import type { CreateTaskInput, Task, TaskPriority } from "../api/types";
import { TASK_PRIORITIES, PRIORITY_LABELS, isTaskPriority } from "../api/types";
import { todayIso } from "../format";

interface Props {
  onCreate: (input: CreateTaskInput) => Promise<Task>;
}

export function TaskForm({ onCreate }: Props) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<TaskPriority>("MEDIUM");
  const [dueDate, setDueDate] = useState("");
  const [saving, setSaving] = useState(false);
  const [fieldError, setFieldError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const titleRef = useRef<HTMLInputElement>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const cleanTitle = title.trim();
    if (!cleanTitle) {
      setFieldError("El título es obligatorio");
      titleRef.current?.focus();
      return;
    }

    setFieldError(null);
    setFormError(null);
    setSaving(true);
    try {
      await onCreate({
        title: cleanTitle,
        description: description.trim() || null,
        priority,
        dueDate: dueDate || null,
      });
      setTitle("");
      setDescription("");
      setPriority("MEDIUM");
      setDueDate("");
      titleRef.current?.focus();
    } catch (cause) {
      setFormError(cause instanceof Error ? cause.message : "No se pudo crear la tarea");
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="task-form" onSubmit={handleSubmit} noValidate>
      <div className="field">
        <label htmlFor="task-title">Título</label>
        <input
          ref={titleRef}
          id="task-title"
          name="title"
          type="text"
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          maxLength={200}
          placeholder="Ej. Preparar la demo del viernes"
          aria-invalid={fieldError !== null}
          aria-describedby={fieldError ? "task-title-error" : undefined}
          required
        />
        {fieldError && (
          <p id="task-title-error" className="field-error" role="alert">
            {fieldError}
          </p>
        )}
      </div>

      <div className="field">
        <label htmlFor="task-description">Descripción</label>
        <textarea
          id="task-description"
          name="description"
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          maxLength={2000}
          rows={3}
          placeholder="Contexto, criterios de aceptación, enlaces…"
        />
      </div>

      <div className="field-row">
        <div className="field">
          <label htmlFor="task-priority">Prioridad</label>
          <select
            id="task-priority"
            name="priority"
            value={priority}
            onChange={(event) => {
              const value = event.target.value;
              if (isTaskPriority(value)) {
                setPriority(value);
              }
            }}
          >
            {TASK_PRIORITIES.map((value) => (
              <option key={value} value={value}>
                {PRIORITY_LABELS[value]}
              </option>
            ))}
          </select>
        </div>

        <div className="field">
          <label htmlFor="task-due-date">Fecha límite</label>
          <input
            id="task-due-date"
            name="dueDate"
            type="date"
            value={dueDate}
            min={todayIso()}
            onChange={(event) => setDueDate(event.target.value)}
          />
        </div>
      </div>

      {formError && (
        <p className="field-error" role="alert">
          {formError}
        </p>
      )}

      <button type="submit" className="primary-button" disabled={saving}>
        {saving ? "Creando…" : "Crear tarea"}
      </button>
    </form>
  );
}
