import { describe, expect, it } from "vitest";
import { clampPercent, daysUntil, formatBytes } from "./format";

describe("clampPercent", () => {
  it("keeps values inside 0..100", () => {
    expect(clampPercent(-5)).toBe(0);
    expect(clampPercent(42.4)).toBe(42);
    expect(clampPercent(250)).toBe(100);
    expect(clampPercent(Number.NaN)).toBe(0);
  });
});

describe("formatBytes", () => {
  it("formats sizes", () => {
    expect(formatBytes(512)).toBe("512 B");
    expect(formatBytes(2048)).toBe("2.0 KB");
    expect(formatBytes(5 * 1024 * 1024)).toBe("5.0 MB");
    expect(formatBytes(-1)).toBe("");
  });
});

describe("daysUntil", () => {
  const today = new Date(2026, 9, 6, 15, 30);

  it("counts whole days, negative when overdue", () => {
    expect(daysUntil("2026-10-06", today)).toBe(0);
    expect(daysUntil("2026-10-16", today)).toBe(10);
    expect(daysUntil("2026-10-01", today)).toBe(-5);
  });

  it("returns null without a usable deadline", () => {
    expect(daysUntil(null, today)).toBeNull();
    expect(daysUntil("not-a-date", today)).toBeNull();
  });
});
