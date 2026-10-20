const dayMonth = new Intl.DateTimeFormat("es", { day: "2-digit", month: "short" });
const fullDate = new Intl.DateTimeFormat("es", {
  weekday: "short",
  day: "2-digit",
  month: "short",
  year: "numeric",
});
const shortWeek = new Intl.DateTimeFormat("es", { day: "2-digit", month: "2-digit" });

function parseDateOnly(value: string): Date {
  return new Date(`${value}T00:00:00`);
}

export function formatDayMonth(value: string): string {
  return dayMonth.format(parseDateOnly(value));
}

export function formatFullDate(value: string): string {
  return fullDate.format(parseDateOnly(value));
}

export function formatWeek(value: string): string {
  return shortWeek.format(parseDateOnly(value));
}

export function formatVolume(value: number | undefined | null): string {
  if (!value) {
    return "0 kg";
  }
  return `${Math.round(value).toLocaleString("es-CL")} kg`;
}

export function formatNumber(value: number | undefined | null): string {
  return Math.round(value ?? 0).toLocaleString("es-CL");
}

export function formatMinutes(value: number | undefined | null): string {
  const total = value ?? 0;
  if (total < 60) {
    return `${total} min`;
  }
  const hours = Math.floor(total / 60);
  const minutes = total % 60;
  return minutes === 0 ? `${hours} h` : `${hours} h ${minutes} min`;
}

export function todayIso(): string {
  const now = new Date();
  const offset = now.getTimezoneOffset();
  return new Date(now.getTime() - offset * 60_000).toISOString().slice(0, 10);
}

export function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

export function greeting(date = new Date()): string {
  const hour = date.getHours();
  if (hour < 12) {
    return "Buenos días";
  }
  if (hour < 20) {
    return "Buenas tardes";
  }
  return "Buenas noches";
}

export function relativeDay(value: string): string {
  const today = todayIso();
  if (value === today) {
    return "Hoy";
  }
  const yesterday = new Date(Date.now() - 86_400_000).toISOString().slice(0, 10);
  if (value === yesterday) {
    return "Ayer";
  }
  return formatDayMonth(value);
}
