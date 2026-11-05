import { describe, expect, it } from "vitest";
import { formatScore, hasSourceProblems, providerLabel, SIMILARITY_LABELS } from "./research";

describe("research helpers", () => {
  it("never labels a result as original", () => {
    const text = Object.values(SIMILARITY_LABELS).join(" ").toLowerCase();
    expect(text).not.toContain("original");
    expect(SIMILARITY_LABELS.insufficient_data).toBe("Not enough data");
  });

  it("detects sources that had problems", () => {
    expect(hasSourceProblems([{ status: "ok" }, { status: "no_results" }])).toBe(false);
    expect(hasSourceProblems([{ status: "ok" }, { status: "unavailable" }])).toBe(true);
    expect(hasSourceProblems([{ status: "rate_limited" }])).toBe(true);
  });

  it("labels providers and formats scores", () => {
    expect(providerLabel("semantic_scholar")).toBe("Semantic Scholar");
    expect(providerLabel("other")).toBe("other");
    expect(formatScore(0.4413)).toBe("0.44");
    expect(formatScore(Number.NaN)).toBe("");
  });
});
