export const DISCIPLINES = ["CROSSFIT", "POWERLIFTING", "CALISTHENICS", "HYROX"] as const;
export type Discipline = (typeof DISCIPLINES)[number];

export const SESSION_TYPES = ["STRENGTH", "WOD", "HYROX_SIM", "SKILL", "CARDIO", "MIXED"] as const;
export type SessionType = (typeof SESSION_TYPES)[number];

export const WOD_FORMATS = ["AMRAP", "EMOM", "FOR_TIME", "INTERVAL"] as const;
export type WodFormat = (typeof WOD_FORMATS)[number];

export const WOD_DIVISIONS = ["RX", "INTERMEDIATE", "SCALED", "OPEN", "PRO", "DOUBLES", "RELAY"] as const;
export type WodDivision = (typeof WOD_DIVISIONS)[number];

export type AthleteTier = "OPEN" | "ELITE";
export type Sex = "FEMALE" | "MALE" | "OTHER";
export type LeaderboardMetric = "VOLUME" | "SESSIONS" | "MINUTES";

export interface Athlete {
  id: string;
  handle: string;
  displayName: string;
  sex: Sex;
  birthDate?: string;
  bodyweightKg?: number;
  country?: string;
  bio?: string;
  tier: AthleteTier;
  disciplines: Discipline[];
}

export interface Exercise {
  id: string;
  slug: string;
  name: string;
  discipline: Discipline;
  category: string;
  unit: "KG" | "BODYWEIGHT" | "REPS" | "SECONDS" | "METERS";
  benchmark: boolean;
}

export interface SessionEntry {
  id: string;
  exerciseId?: string;
  movement: string;
  setIndex: number;
  reps?: number;
  weightKg?: number;
  seconds?: number;
  rpe?: number;
}

export interface TrainingSession {
  id: string;
  performedOn: string;
  discipline: Discipline;
  sessionType: SessionType;
  title: string;
  notes?: string;
  durationMin?: number;
  rpe?: number;
  wodFormat?: WodFormat;
  wodScore?: string;
  wodDivision?: WodDivision;
  createdAt: string;
  volumeKg: number;
  entries: SessionEntry[];
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

export interface WeeklyPoint {
  weekStart: string;
  sessions: number;
  minutes: number;
  volumeKg: number;
}

export interface PersonalRecord {
  exerciseId?: string;
  movement: string;
  bestWeightKg?: number;
  bestReps?: number;
  bestSeconds?: number;
  achievedOn: string;
}

export interface DisciplineBreakdown {
  discipline: Discipline;
  sessions: number;
  volumeKg: number;
}

export interface Stats {
  athlete: Athlete;
  totals: { sessions: number; minutes: number; volumeKg: number; prs: number };
  weekly: WeeklyPoint[];
  personalRecords: PersonalRecord[];
  disciplines: DisciplineBreakdown[];
}

export interface LeaderboardEntry {
  rank: number;
  athleteId: string;
  displayName: string;
  handle: string;
  tier: AthleteTier;
  value: number;
  sessions: number;
  me: boolean;
}

export interface SessionEntryInput {
  exerciseId?: string | null;
  movement?: string | null;
  setIndex?: number;
  reps?: number | null;
  weightKg?: number | null;
  seconds?: number | null;
  rpe?: number | null;
}

export interface CreateSessionInput {
  performedOn: string;
  discipline: Discipline;
  sessionType: SessionType;
  title: string;
  notes?: string | null;
  durationMin?: number | null;
  rpe?: number | null;
  wodFormat?: WodFormat | null;
  wodScore?: string | null;
  wodDivision?: WodDivision | null;
  entries?: SessionEntryInput[];
}

export const DISCIPLINE_LABELS: Record<Discipline, string> = {
  CROSSFIT: "CrossFit",
  POWERLIFTING: "Powerlifting",
  CALISTHENICS: "Calistenia",
  HYROX: "HYROX",
};

export const SESSION_TYPE_LABELS: Record<SessionType, string> = {
  STRENGTH: "Fuerza",
  WOD: "WOD",
  HYROX_SIM: "Simulación HYROX",
  SKILL: "Skills",
  CARDIO: "Cardio",
  MIXED: "Mixto",
};

export const WOD_FORMAT_LABELS: Record<WodFormat, string> = {
  AMRAP: "AMRAP",
  EMOM: "EMOM",
  FOR_TIME: "For Time",
  INTERVAL: "Intervalos",
};

export const DIVISION_LABELS: Record<WodDivision, string> = {
  RX: "RX",
  INTERMEDIATE: "Intermedio",
  SCALED: "Scaled",
  OPEN: "Open",
  PRO: "Pro",
  DOUBLES: "Dobles",
  RELAY: "Relay",
};

export const METRIC_LABELS: Record<LeaderboardMetric, string> = {
  VOLUME: "Volumen",
  SESSIONS: "Sesiones",
  MINUTES: "Minutos",
};

export function isDiscipline(value: string): value is Discipline {
  return (DISCIPLINES as readonly string[]).includes(value);
}

export function isSessionType(value: string): value is SessionType {
  return (SESSION_TYPES as readonly string[]).includes(value);
}

export function isWodFormat(value: string): value is WodFormat {
  return (WOD_FORMATS as readonly string[]).includes(value);
}

export function isWodDivision(value: string): value is WodDivision {
  return (WOD_DIVISIONS as readonly string[]).includes(value);
}
