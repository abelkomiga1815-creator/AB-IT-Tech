/** Shared by middleware (edge runtime) and route handlers: no Node-only imports here. */
export const SESSION_COOKIE = "abit_student_session";
export const PROTECTED_PREFIXES = ["/dashboard", "/projects", "/profile"] as const;
