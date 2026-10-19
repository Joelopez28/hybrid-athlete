import type {
  ApiErrorBody,
  CreateTaskInput,
  Page,
  Task,
  TaskStatus,
  UpdateTaskInput,
} from "./types";

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

interface ListParams {
  status?: TaskStatus;
  page?: number;
  size?: number;
  signal?: AbortSignal;
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
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
      const body = (await response.json()) as Partial<ApiErrorBody>;
      if (body.message) {
        message = body.message;
      }
    } catch {
      // The error body is not JSON: keep the status-based message.
    }
    throw new ApiError(response.status, message);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export const api = {
  listTasks({ status, page = 0, size = 20, signal }: ListParams = {}): Promise<Page<Task>> {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (status) {
      params.set("status", status);
    }
    return request<Page<Task>>(`/tasks?${params.toString()}`, { signal });
  },

  createTask(input: CreateTaskInput, signal?: AbortSignal): Promise<Task> {
    return request<Task>("/tasks", {
      method: "POST",
      body: JSON.stringify(input),
      signal,
    });
  },

  updateTask(id: string, patch: UpdateTaskInput): Promise<Task> {
    return request<Task>(`/tasks/${id}`, {
      method: "PATCH",
      body: JSON.stringify(patch),
    });
  },

  deleteTask(id: string): Promise<void> {
    return request<void>(`/tasks/${id}`, { method: "DELETE" });
  },
};
