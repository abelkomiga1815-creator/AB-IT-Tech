import { describe, expect, it } from "vitest";
import { safeHttpUrl } from "./safe-url";

describe("safeHttpUrl", () => {
  it("accepts http and https links", () => {
    expect(safeHttpUrl("https://doi.org/10.1/abc")).toBe("https://doi.org/10.1/abc");
    expect(safeHttpUrl("http://example.org/paper")).toBe("http://example.org/paper");
  });

  it("refuses every other scheme and malformed values", () => {
    for (const value of ["javascript:alert(1)", "data:text/html,<script>alert(1)</script>", "ftp://x.org/p",
      "//evil.example", "not a url", "", null, undefined]) {
      expect(safeHttpUrl(value as string | null | undefined)).toBeNull();
    }
  });
});
