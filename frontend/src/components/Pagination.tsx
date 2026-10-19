interface Props {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}

export function Pagination({ page, totalPages, onChange }: Props) {
  if (totalPages <= 1) {
    return null;
  }

  return (
    <nav className="pagination" aria-label="Paginación de tareas">
      <button
        type="button"
        className="secondary-button"
        onClick={() => onChange(page - 1)}
        disabled={page <= 0}
      >
        Anterior
      </button>
      <span className="page-indicator">
        Página {page + 1} de {totalPages}
      </span>
      <button
        type="button"
        className="secondary-button"
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages - 1}
      >
        Siguiente
      </button>
    </nav>
  );
}
