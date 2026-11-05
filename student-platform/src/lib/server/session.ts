import "server-only";
import type { NextResponse } from "next/server";
import { SESSION_COOKIE } from "@/lib/session-constants";

/** Stores the backend JWT in an httpOnly cookie: JavaScript in the page can never read it. */
export function setSessionCookie(response: NextResponse, token: string, maxAgeSeconds: number): void {
  response.cookies.set(SESSION_COOKIE, token, {
    httpOnly: true,
    sameSite: "strict",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: maxAgeSeconds,
  });
}

export function clearSessionCookie(response: NextResponse): void {
  response.cookies.set(SESSION_COOKIE, "", { httpOnly: true, sameSite: "strict", path: "/", maxAge: 0 });
}
