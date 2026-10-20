interface Props {
  label: string;
  value: string;
  hint?: string;
  accent?: "lime" | "blue" | "orange" | "neutral";
}

export function StatCard({ label, value, hint, accent = "neutral" }: Props) {
  return (
    <article className={`stat-card stat-${accent}`}>
      <p className="stat-label">{label}</p>
      <p className="stat-value">{value}</p>
      {hint && <p className="stat-hint">{hint}</p>}
    </article>
  );
}
