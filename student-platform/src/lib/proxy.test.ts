import { describe, expect, it } from "vitest";
import { isAllowedBackendPath } from "./proxy";

describe("isAllowedBackendPath", () => {
  it("allows the endpoints the platform needs", () => {
    expect(isAllowedBackendPath(["auth", "me"])).toBe(true);
    expect(isAllowedBackendPath(["profile"])).toBe(true);
    expect(isAllowedBackendPath(["student", "profile"])).toBe(true);
    expect(isAllowedBackendPath(["student", "projects"])).toBe(true);
    expect(
      isAllowedBackendPath(["student", "projects", "3f6c1a10-0001-4b1e-9a00-000000000001", "documents"]),
    ).toBe(true);
    expect(isAllowedBackendPath(["services"])).toBe(true);
  });

  it("blocks everything else, including login/register and admin and customer areas", () => {
    expect(isAllowedBackendPath(["auth", "login"])).toBe(false);
    expect(isAllowedBackendPath(["auth", "register"])).toBe(false);
    expect(isAllowedBackendPath(["admin", "inquiries"])).toBe(false);
    expect(isAllowedBackendPath(["customer", "profile"])).toBe(false);
    expect(isAllowedBackendPath(["actuator", "health"])).toBe(false);
    expect(isAllowedBackendPath(["student"])).toBe(false);
    expect(isAllowedBackendPath(["profile", "extra"])).toBe(false);
    expect(isAllowedBackendPath([])).toBe(false);
  });

  it("rejects path traversal and unusual characters", () => {
    expect(isAllowedBackendPath(["student", "..", "admin"])).toBe(false);
    expect(isAllowedBackendPath(["student", ".", "projects"])).toBe(false);
    expect(isAllowedBackendPath(["student", "projects/../../admin"])).toBe(false);
    expect(isAllowedBackendPath(["student", "a%2e%2e"])).toBe(false);
    expect(isAllowedBackendPath(["student", "projects", ""])).toBe(false);
    expect(isAllowedBackendPath(["student", "a\\b"])).toBe(false);
  });

  it("rejects absurdly deep paths", () => {
    expect(isAllowedBackendPath(["student", "a", "b", "c", "d", "e", "f", "g", "h"])).toBe(false);
  });
});
