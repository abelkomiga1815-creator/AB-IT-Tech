import "server-only";
import { NextResponse } from "next/server";
import type { User } from "@/lib/types";
import { BACKEND_URL, errorBody } from "./backend";
import { setSessionCookie } from "./session";

interface LoginPayload {
  success: boolean;
  data?: { accessToken: string; expiresIn: number; user: User };
}

/**
 * Logs in against the backend and, for STUDENT accounts only, starts a session cookie.
 * Shared by the login and register routes.
 */
export async function loginStudent(email: string, password: string): Promise<Response> {
  let upstream: Response;
  try {
    upstream = await fetch(`${BACKEND_URL}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
      cache: "no-store",
    });
  } catch {
    return Response.json(errorBody("502", "BACKEND_UNAVAILABLE"), { status: 502 });
  }

  const payload = (await upstream.json().catch(() => null)) as LoginPayload | null;
  if (!upstream.ok || !payload?.success || !payload.data) {
    return Response.json(payload ?? { success: false, code: "UNKNOWN_ERROR", message: "Login failed." }, {
      status: upstream.ok ? 502 : upstream.status,
    });
  }
  if (payload.data.user.role !== "STUDENT") {
    return Response.json(errorBody("403", "NOT_A_STUDENT_ACCOUNT"));
  }

  const response = NextResponse.json({ success: true, data: { user: payload.data.user } });
  setSessionCookie(response, payload.data.accessToken, payload.data.expiresIn);
  return response;
}
