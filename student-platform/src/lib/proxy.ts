/**
 * Allow-list for the backend proxy (/api/backend/...). Only the endpoints this platform needs can be
 * reached, and path tricks are rejected, so the proxy cannot be used to call arbitrary backend URLs.
 */
export function isAllowedBackendPath(segments: string[]): boolean {
  if (segments.length === 0 || segments.length > 8) {
    return false;
  }
  const safe = segments.every(
    (segment) => /^[A-Za-z0-9._-]+$/.test(segment) && segment !== "." && segment !== "..",
  );
  if (!safe) {
    return false;
  }
  const [first, second] = segments;
  switch (first) {
    case "auth":
      return segments.length === 2 && second === "me";
    case "profile":
      return segments.length === 1;
    case "student":
      return segments.length >= 2;
    case "services":
      return true;
    default:
      return false;
  }
}
