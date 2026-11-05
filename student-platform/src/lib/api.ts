/**
 * Browser-side API client. Every call goes to the same-origin proxy (/api/backend/...), which adds
 * the session token; pages and components never call the backend directly.
 */

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
    public readonly fieldErrors: { field: string; message: string }[] = [],
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function parse<T>(response: Response): Promise<T> {
  const payload = (await response.json().catch(() => null)) as
    | { success: true; data: T }
    | { success: false; code: string; message: string; errors?: { field: string; message: string }[] }
    | null;

  if (response.status === 401 && typeof window !== "undefined" && window.location.pathname !== "/login") {
    window.location.href = "/login";
  }
  if (!response.ok || payload === null || payload.success === false) {
    const failure = payload && payload.success === false ? payload : null;
    throw new ApiError(
      response.status,
      failure?.code ?? "UNKNOWN_ERROR",
      failure?.message ?? "Something went wrong. Please try again.",
      failure?.errors ?? [],
    );
  }
  return payload.data;
}

async function call<T>(method: string, path: string, body?: unknown): Promise<T> {
  const response = await fetch(`/api/backend/${path}`, {
    method,
    headers: body !== undefined ? { "Content-Type": "application/json" } : undefined,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  return parse<T>(response);
}

export const api = {
  get: <T>(path: string) => call<T>("GET", path),
  post: <T>(path: string, body?: unknown) => call<T>("POST", path, body ?? {}),
  put: <T>(path: string, body: unknown) => call<T>("PUT", path, body),
  delete: (path: string) => call<void>("DELETE", path),

  /** Uploads a project document (multipart). The browser sets the multipart boundary itself. */
  async upload<T>(path: string, form: FormData): Promise<T> {
    const response = await fetch(`/api/backend/${path}`, { method: "POST", body: form });
    return parse<T>(response);
  },
};

/** Auth routes (they set or clear the httpOnly session cookie, so they are not proxied calls). */
export const auth = {
  async login(email: string, password: string): Promise<void> {
    await parse(await fetch("/api/auth/login", jsonInit({ email, password })));
  },
  async register(fullName: string, email: string, password: string): Promise<{ signedIn: boolean }> {
    const result = await parse<{ user: unknown }>(await fetch("/api/auth/register", jsonInit({ fullName, email, password })));
    return { signedIn: result.user !== null };
  },
  async logout(): Promise<void> {
    await fetch("/api/auth/logout", { method: "POST" });
  },
};

function jsonInit(body: unknown): RequestInit {
  return { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) };
}

export function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : "Something went wrong. Please try again.";
}

/** Maps field-level validation errors from the backend to `{ field: message }`. */
export function fieldErrorsOf(error: unknown): Record<string, string> {
  if (!(error instanceof ApiError)) {
    return {};
  }
  return Object.fromEntries(error.fieldErrors.map((e) => [e.field, e.message]));
}
