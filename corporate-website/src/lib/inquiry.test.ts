import { describe, expect, it } from "vitest";
import { buildInquiryMailto, toGuestInquiryPayload, validateInquiry, type InquiryValues } from "./inquiry";

const valid: InquiryValues = {
  name: "Jane Doe",
  email: "jane@example.com",
  phone: "+255 712 345 678",
  service: "web-development",
  message: "We need a website for our company.",
};

describe("validateInquiry", () => {
  it("accepts a complete valid request", () => {
    expect(validateInquiry(valid)).toEqual({});
  });

  it("allows an empty phone number", () => {
    expect(validateInquiry({ ...valid, phone: "" })).toEqual({});
  });

  it("reports every invalid field", () => {
    const errors = validateInquiry({ name: "J", email: "nope", phone: "abc", service: "", message: "short" });
    expect(Object.keys(errors).sort()).toEqual(["email", "message", "name", "phone"]);
  });
});

describe("toGuestInquiryPayload", () => {
  it("matches the backend's guest inquiry body and trims values", () => {
    expect(toGuestInquiryPayload({ ...valid, name: "  Jane Doe ", message: " We need a website. " })).toEqual({
      name: "Jane Doe",
      email: "jane@example.com",
      phone: "+255 712 345 678",
      serviceSlug: "web-development",
      message: "We need a website.",
    });
  });

  it("leaves out empty phone and unknown or empty services", () => {
    const payload = toGuestInquiryPayload({ ...valid, phone: " ", service: "" });
    expect(payload).not.toHaveProperty("phone");
    expect(payload).not.toHaveProperty("serviceSlug");
    expect(toGuestInquiryPayload({ ...valid, service: "time-travel" })).not.toHaveProperty("serviceSlug");
  });
});

describe("buildInquiryMailto", () => {
  it("encodes subject and body safely and names the service by its title", () => {
    const url = buildInquiryMailto("info@example.com", { ...valid, message: "Hello & welcome\nSecond line" });
    expect(url.startsWith("mailto:info@example.com?subject=")).toBe(true);
    expect(url).toContain(encodeURIComponent("Service request: Web Development"));
    expect(url).toContain(encodeURIComponent("Hello & welcome\nSecond line"));
    expect(url).not.toContain(" ");
  });

  it("uses a generic subject when no service is chosen", () => {
    const url = buildInquiryMailto("info@example.com", { ...valid, service: "" });
    expect(url).toContain(encodeURIComponent("Website inquiry"));
  });
});
