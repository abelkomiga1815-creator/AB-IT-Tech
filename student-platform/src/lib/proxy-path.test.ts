import { describe, expect, it } from "vitest";
import { isAllowedBackendPath } from "./proxy-path";

describe("isAllowedBackendPath", () => {
  it("allows the areas the student app needs", () => {
    expect(isAllowedBackendPath(["auth", "me"])).toBe(true);
    expect(isAllowedBackendPath(["profile"])).toBe(true);
    expect(isAllowedBackendPath(["services"])).toBe(true);
    expect(isAllowedBackendPath(["student", "projects"])).toBe(true);
    expect(isAllowedBackendPath(["student", "projects", "abc", "documents", "def", "download"])).toBe(true);
  });

  it("does not proxy login or registration (they set the session cookie elsewhere)", () => {
    expect(isAllowedBackendPath(["auth", "login"])).toBe(false);
    expect(isAllowedBackendPath(["auth", "register"])).toBe(false);
    expect(isAllowedBackendPath(["auth"])).toBe(false);
  });

  it("blocks customer, admin, actuator and unknown areas", () => {
    for (const path of [["customer", "profile"], ["admin", "inquiries"], ["actuator", "health"], ["inquiries"], ["profile", "x"]]) {
      expect(isAllowedBackendPath(path)).toBe(false);
    }
  });

  it("blocks traversal and malformed segments", () => {
    expect(isAllowedBackendPath([])).toBe(false);
    expect(isAllowedBackendPath(["student", "..", "admin"])).toBe(false);
    expect(isAllowedBackendPath(["student", "", "projects"])).toBe(false);
    expect(isAllowedBackendPath(["student", "a/b"])).toBe(false);
    expect(isAllowedBackendPath(["student", "a\\b"])).toBe(false);
  });
});
