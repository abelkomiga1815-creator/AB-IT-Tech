import { API_BASE_URL } from "@/config/site";

/**
 * Small typed client for the Spring Boot backend (GROUP 3). All HTTP calls to the backend go
 * through this file; pages and components contain no fetch calls and no business logic.
 */

export interface ApiSuccess<T> {
  success: true;
  data: T;
}

export interface ApiFailure {
  success: false;
  code: string;
  message: string;
  timestamp: string;
  errors?: { field: string; message: string }[];
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}/api/v1${path}`, {
    ...init,
    headers: { "Content-Type": "application/json", ...(init.headers ?? {}) },
  });
  const body = (await response.json().catch(() => null)) as ApiSuccess<T> | ApiFailure | null;
  if (!response.ok || body === null || body.success === false) {
    const failure = body && body.success === false ? body : null;
    throw new ApiError(
      response.status,
      failure?.code ?? "UNKNOWN_ERROR",
      failure?.message ?? "The service is temporarily unavailable.",
    );
  }
  return body.data;
}

export interface HealthStatus {
  success: boolean;
  status: string;
}

/** The health endpoint returns {success, status} at the top level instead of a `data` envelope. */
export async function getBackendHealth(): Promise<HealthStatus> {
  const response = await fetch(`${API_BASE_URL}/api/v1/health`, { cache: "no-store" });
  if (!response.ok) {
    throw new ApiError(response.status, "HEALTH_CHECK_FAILED", "Backend is not healthy.");
  }
  return (await response.json()) as HealthStatus;
}

export interface InquiryReceipt {
  id: string;
  status: string;
  createdAt: string;
}

/**
 * Sends a website inquiry to the backend. The backend must list this site's origin in
 * CORS_ALLOWED_ORIGINS. The answer contains no personal data.
 */
export async function submitGuestInquiry(payload: {
  name: string;
  email: string;
  phone?: string;
  serviceSlug?: string;
  message: string;
}): Promise<InquiryReceipt> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}/api/v1/inquiries`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
  } catch {
    throw new ApiError(0, "NETWORK_ERROR", "We could not reach our server. Please try again in a moment.");
  }
  const body = (await response.json().catch(() => null)) as ApiSuccess<InquiryReceipt> | ApiFailure | null;
  if (!response.ok || body === null || body.success === false) {
    const failure = body && body.success === false ? body : null;
    const message =
      response.status === 429
        ? "Too many requests. Please wait a minute and try again."
        : failure?.code === "VALIDATION_ERROR"
          ? "Please check the highlighted details and try again."
          : "We could not send your request. Please try again in a moment.";
    throw new ApiError(response.status, failure?.code ?? "UNKNOWN_ERROR", message);
  }
  return body.data;
}
