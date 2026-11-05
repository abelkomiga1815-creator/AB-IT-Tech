import { describe, expect, it } from "vitest";
import { DEFENSE_CATEGORIES, DOCUMENT_TYPES, PROJECT_STAGES, PROJECT_STATUSES, labelOf } from "./constants";
import { validateUploadFile } from "./upload";

describe("enum lists match the backend", () => {
  it("project stages are in the backend order", () => {
    expect(PROJECT_STAGES.map((s) => s.value)).toEqual([
      "TOPIC_SELECTION", "PROPOSAL", "LITERATURE_REVIEW", "METHODOLOGY", "IMPLEMENTATION",
      "WRITING", "REVIEW", "DEFENSE_PREPARATION", "SUBMISSION",
    ]);
  });

  it("statuses, document types and categories match", () => {
    expect(PROJECT_STATUSES.map((s) => s.value)).toEqual(["PLANNING", "IN_PROGRESS", "COMPLETED", "ARCHIVED"]);
    expect(DOCUMENT_TYPES.map((s) => s.value)).toEqual(
      ["PROPOSAL", "THESIS_DRAFT", "REFERENCE", "DATA", "PRESENTATION", "OTHER"]);
    expect(DEFENSE_CATEGORIES.map((s) => s.value)).toEqual(
      ["GENERAL", "METHODOLOGY", "LITERATURE", "RESULTS", "IMPLEMENTATION", "OTHER"]);
  });

  it("finds labels", () => {
    expect(labelOf(PROJECT_STAGES, "LITERATURE_REVIEW")).toBe("Literature review");
    expect(labelOf(PROJECT_STAGES, null)).toBe("");
  });
});

describe("validateUploadFile", () => {
  it("accepts allowed documents", () => {
    expect(validateUploadFile({ name: "thesis.PDF", size: 1000 })).toBeNull();
    expect(validateUploadFile({ name: "data.csv", size: 10 * 1024 * 1024 })).toBeNull();
  });

  it("rejects empty, oversized and disallowed files", () => {
    expect(validateUploadFile({ name: "a.pdf", size: 0 })).toMatch(/empty/);
    expect(validateUploadFile({ name: "a.pdf", size: 10 * 1024 * 1024 + 1 })).toMatch(/too large/);
    expect(validateUploadFile({ name: "virus.exe", size: 100 })).toMatch(/not allowed/);
    expect(validateUploadFile({ name: "README", size: 100 })).toMatch(/not allowed/);
  });
});
