import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { BACKEND_URL, SESSION_COOKIE, clientAddressHeaders, errorBody, sessionCookieOptions } from "@/lib/server/backend";

/**
 * Registers a STUDENT account and signs it in. The role is fixed here on the server: whatever the
 * browser sends is ignored, so this form can never create another kind of account.
 */
export async function POST(request: Request) {
  const body = (await request.json().catch(() => null)) as
    | { fullName?: unknown; email?: unknown; password?: unknown }
    | null;
  if (!body || typeof body.fullName !== "string" || typeof body.email !== "string" || typeof body.password !== "string") {
    return NextResponse.json(errorBody("BAD_REQUEST", "Full name, email and password are required."), { status: 400 });
  }

  const json = { "Content-Type": "application/json", ...clientAddressHeaders(request) };
  try {
    const registered = await fetch(`${BACKEND_URL}/api/v1/auth/register`, {
      method: "POST",
      headers: json,
      body: JSON.stringify({ fullName: body.fullName, email: body.email, password: body.password, role: "STUDENT" }),
      cache: "no-store",
    });
    if (!registered.ok) {
      const failure = await registered.json().catch(() => null);
      return NextResponse.json(
        failure ?? errorBody("SERVICE_UNAVAILABLE", "The service is temporarily unavailable."),
        { status: registered.status },
      );
    }

    const login = await fetch(`${BACKEND_URL}/api/v1/auth/login`, {
      method: "POST",
      headers: json,
      body: JSON.stringify({ email: body.email, password: body.password }),
      cache: "no-store",
    });
    const payload = await login.json().catch(() => null);
    if (!login.ok || !payload?.success) {
      // The account exists; ask the student to sign in manually.
      return NextResponse.json({ success: true, data: { user: null } }, { status: 201 });
    }
    const { accessToken, expiresIn, user } = payload.data;
    (await cookies()).set(SESSION_COOKIE, accessToken, sessionCookieOptions(expiresIn));
    return NextResponse.json({ success: true, data: { user } }, { status: 201 });
  } catch {
    return NextResponse.json(errorBody("SERVICE_UNAVAILABLE", "The service is temporarily unavailable."), { status: 502 });
  }
}
