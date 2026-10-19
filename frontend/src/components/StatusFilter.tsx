import type { TaskFilter } from "../hooks/useTasks";
import { pluralize } from "../format";

interface Props {
  value: TaskFilter;
  total: number | null;
  onChange: (filter: TaskFilter) => void;
}

const FILTERS: { value: TaskFilter; label: string }[] = [
  { value: "ALL", label: "Todas" },
  { value: "TODO", label: "Pendientes" },
  { value: "IN_PROGRESS", label: "En curso" },
  { value: "DONE", label: "Hechas" },
];

export function StatusFilter({ value, total, onChange }: Props) {
  return (
    <div className="filters" role="group" aria-label="Filtrar tareas por estado">
      {FILTERS.map((filter) => (
        <button
          key={filter.value}
          type="button"
          className="filter-chip"
          aria-pressed={value === filter.value}
          onClick={() => onChange(filter.value)}
        >
          {filter.label}
        </button>
      ))}
      <span className="filter-count" aria-live="polite">
        {total === null ? "…" : pluralize(total, "tarea", "tareas")}
      </span>
    </div>
  );
}
