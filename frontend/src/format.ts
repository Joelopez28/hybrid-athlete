import type { TaskStatus } from "./api/types";

const dateFormatter = new Intl.DateTimeFormat("es", {
  day: "2-digit",
  month: "short",
  year: "numeric",
  timeZone: "UTC",
});

const localDateFormatter = new Intl.DateTimeFormat("es", {
  day: "2-digit",
  month: "short",
  year: "numeric",
});

export function formatDate(value: string | null): string {
  if (!value) {
    return "Sin fecha";
  }
  const date = new Date(`${value}T00:00:00Z`);
  return Number.isNaN(date.getTime()) ? "Fecha inválida" : dateFormatter.format(date);
}

export function formatLocalDate(isoTimestamp: string): string {
  const date = new Date(isoTimestamp);
  return Number.isNaN(date.getTime()) ? "Fecha inválida" : localDateFormatter.format(date);
}

export function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

export function isOverdue(dueDate: string | null, status: TaskStatus): boolean {
  if (!dueDate) {
    return false;
  }
  return dueDate < todayIso() && status !== "DONE";
}

export function pluralize(count: number, singular: string, plural: string): string {
  return `${count} ${count === 1 ? singular : plural}`;
}
