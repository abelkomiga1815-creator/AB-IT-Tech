import { ALLOWED_UPLOAD_EXTENSIONS, MAX_UPLOAD_BYTES } from "./constants";
import { formatBytes } from "./format";

/**
 * Client-side pre-check for uploads. Returns an error message, or null when the file looks acceptable.
 * The server repeats these checks; this only saves a round trip.
 */
export function validateUploadFile(file: { name: string; size: number }): string | null {
  if (file.size <= 0) {
    return "The file is empty.";
  }
  if (file.size > MAX_UPLOAD_BYTES) {
    return `The file is too large (maximum ${formatBytes(MAX_UPLOAD_BYTES)}).`;
  }
  const dot = file.name.lastIndexOf(".");
  const extension = dot >= 0 ? file.name.slice(dot + 1).toLowerCase() : "";
  if (!(ALLOWED_UPLOAD_EXTENSIONS as readonly string[]).includes(extension)) {
    return `This file type is not allowed. Allowed: ${ALLOWED_UPLOAD_EXTENSIONS.join(", ")}.`;
  }
  return null;
}
