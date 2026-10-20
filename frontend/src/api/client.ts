import type {
  Athlete,
  AthleteTier,
  CreateSessionInput,
  Discipline,
  Exercise,
  LeaderboardEntry,
  LeaderboardMetric,
  Page,
  Stats,
  TrainingSession,
} from "./types";

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

interface RequestOptions extends RequestInit {
  signal?: AbortSignal;
}

async function request<T>(path: string, init: RequestOptions = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  let response: Response;
  try {
    response = await fetch(`/api${path}`, { ...init, headers });
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === "AbortError") {
      throw cause;
    }
    throw new ApiError(0, "No se pudo conectar con el servidor. ¿Está corriendo el backend?");
  }

  if (!response.ok) {
    let message = `Error ${response.status}`;
    try {
      const body = (await response.json()) as { message?: string };
      if (body.message) {
        message = body.message;
      }
    } catch {
      // Non-JSON error body.
    }
    throw new ApiError(response.status, message);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

function query(params: Record<string, string | number | undefined | null>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, String(value));
    }
  }
  const encoded = search.toString();
  return encoded ? `?${encoded}` : "";
}

export const api = {
  me(signal?: AbortSignal): Promise<Athlete> {
    return request<Athlete>("/me", { signal });
  },

  exercises(signal?: AbortSignal): Promise<Exercise[]> {
    return request<Exercise[]>("/exercises", { signal });
  },

  sessions(limit = 50, signal?: AbortSignal): Promise<Page<TrainingSession>> {
    return request<Page<TrainingSession>>(`/sessions?size=${limit}`, { signal });
  },

  stats(weeks = 8, signal?: AbortSignal): Promise<Stats> {
    return request<Stats>(`/stats?weeks=${weeks}`, { signal });
  },

  leaderboard(
    weeks: number,
    metric: LeaderboardMetric,
    tier?: AthleteTier | null,
    signal?: AbortSignal,
  ): Promise<LeaderboardEntry[]> {
    return request<LeaderboardEntry[]>(
      `/leaderboard${query({ weeks, metric, tier })}`,
      { signal },
    );
  },

  createSession(input: CreateSessionInput): Promise<TrainingSession> {
    return request<TrainingSession>("/sessions", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  deleteSession(id: string): Promise<void> {
    return request<void>(`/sessions/${id}`, { method: "DELETE" });
  },

  exercisesByDiscipline(discipline: Discipline, signal?: AbortSignal): Promise<Exercise[]> {
    return request<Exercise[]>(`/exercises${query({ discipline })}`, { signal });
  },
};
