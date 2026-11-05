import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { BACKEND_URL, SESSION_COOKIE, clientAddressHeaders, errorBody, sessionCookieOptions } from "@/lib/server/backend";

/**
 * Logs a student in. The backend JWT is stored in an httpOnly cookie, so browser JavaScript never
 * sees it. Accounts that are not STUDENT accounts are refused here: this platform is for students.
 */
export async function POST(request: Request) {
  const body = (await request.json().catch(() => null)) as { email?: unknown; password?: unknown } | null;
  if (!body || typeof body.email !== "string" || typeof body.password !== "string") {
    return NextResponse.json(errorBody("BAD_REQUEST", "Email and password are required."), { status: 400 });
  }

  let upstream: Response;
  try {
    upstream = await fetch(`${BACKEND_URL}/api/v1/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...clientAddressHeaders(request) },
      body: JSON.stringify({ email: body.email, password: body.password }),
      cache: "no-store",
    });
  } catch {
    return NextResponse.json(errorBody("SERVICE_UNAVAILABLE", "The service is temporarily unavailable."), { status: 502 });
  }

  const payload = await upstream.json().catch(() => null);
  if (!upstream.ok || !payload?.success) {
    return NextResponse.json(
      payload ?? errorBody("SERVICE_UNAVAILABLE", "The service is temporarily unavailable."),
      { status: upstream.ok ? 502 : upstream.status },
    );
  }

  const { accessToken, expiresIn, user } = payload.data as {
    accessToken: string;
    expiresIn: number;
    user: { role: string };
  };
  if (user.role !== "STUDENT") {
    return NextResponse.json(
      errorBody("NOT_A_STUDENT_ACCOUNT", "This platform is for student accounts. Please use a student account."),
      { status: 403 },
    );
  }

  (await cookies()).set(SESSION_COOKIE, accessToken, sessionCookieOptions(expiresIn));
  return NextResponse.json({ success: true, data: { user } });
}
