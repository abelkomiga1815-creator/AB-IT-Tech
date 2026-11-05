/**
 * Links come from external sources and from the database, so only http(s) addresses are ever turned
 * into clickable links (never javascript:, data:, etc.).
 */
export function safeHttpUrl(value: string | null | undefined): string | null {
  if (!value) {
    return null;
  }
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:" ? url.toString() : null;
  } catch {
    return null;
  }
}
