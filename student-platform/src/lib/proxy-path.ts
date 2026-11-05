/**
 * Which backend paths the browser may reach through the /api/backend proxy.
 * Login and registration are NOT proxied: they go through the dedicated /api/auth routes, which set
 * the session cookie. Everything else must be one of the allow-listed areas below.
 */
export function isAllowedBackendPath(segments: string[]): boolean {
  if (segments.length === 0) {
    return false;
  }
  for (const segment of segments) {
    if (segment === "" || segment === "." || segment === ".." || /[\\/\u0000]/.test(segment)) {
      return false;
    }
  }
  const [first, second] = segments;
  if (first === "auth") {
    return segments.length === 2 && second === "me";
  }
  if (first === "profile") {
    return segments.length === 1;
  }
  return first === "student" || first === "services";
}
