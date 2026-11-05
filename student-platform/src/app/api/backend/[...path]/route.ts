import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { isAllowedBackendPath } from "@/lib/proxy-path";
import { BACKEND_URL, SESSION_COOKIE, clientAddressHeaders, errorBody } from "@/lib/server/backend";

/**
 * Same-origin proxy to the Spring Boot API. It adds the Authorization header from the httpOnly
 * session cookie, so the browser never handles the token and no CORS configuration is needed.
 * Only allow-listed backend areas can be reached (see lib/proxy-path.ts).
 */
async function proxy(request: Request, context: { params: Promise<{ path: string[] }> }) {
  const { path } = await context.params;
  if (!isAllowedBackendPath(path)) {
    return NextResponse.json(errorBody("RESOURCE_NOT_FOUND", "The requested resource was not found"), { status: 404 });
  }

  const token = (await cookies()).get(SESSION_COOKIE)?.value;
  const search = new URL(request.url).search;
  const target = `${BACKEND_URL}/api/v1/${path.map(encodeURIComponent).join("/")}${search}`;

  const headers = new Headers({ accept: request.headers.get("accept") ?? "application/json", ...clientAddressHeaders(request) });
  const contentType = request.headers.get("content-type");
  if (contentType) {
    headers.set("content-type", contentType);
  }
  if (token) {
    headers.set("authorization", `Bearer ${token}`);
  }

  const hasBody = request.method === "POST" || request.method === "PUT";
  let upstream: Response;
  try {
    upstream = await fetch(target, {
      method: request.method,
      headers,
      body: hasBody ? await request.arrayBuffer() : undefined,
      cache: "no-store",
      redirect: "manual",
    });
  } catch {
    return NextResponse.json(errorBody("SERVICE_UNAVAILABLE", "The service is temporarily unavailable."), { status: 502 });
  }

  const responseHeaders = new Headers();
  for (const name of ["content-type", "content-disposition", "content-length", "cache-control", "x-content-type-options", "x-request-id"]) {
    const value = upstream.headers.get(name);
    if (value) {
      responseHeaders.set(name, value);
    }
  }
  return new Response(upstream.body, { status: upstream.status, headers: responseHeaders });
}

export { proxy as GET, proxy as POST, proxy as PUT, proxy as DELETE };
