/** Server-only settings shared by the route handlers. Never import this from client components. */

export const BACKEND_URL = (process.env.BACKEND_URL ?? "http://localhost:8080").replace(/\/+$/, "");

export const SESSION_COOKIE = "abit_student_session";

export function sessionCookieOptions(maxAgeSeconds: number) {
  return {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "strict" as const,
    path: "/",
    maxAge: maxAgeSeconds,
  };
}

/** Headers that tell the backend which browser client the request came from (used for rate limiting). */
export function clientAddressHeaders(request: Request): Record<string, string> {
  const forwarded = request.headers.get("x-forwarded-for");
  return forwarded ? { "X-Forwarded-For": forwarded } : {};
}

export function errorBody(code: string, message: string) {
  return { success: false, code, message, timestamp: new Date().toISOString() };
}
