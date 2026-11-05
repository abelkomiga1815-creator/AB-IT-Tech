import { getServiceBySlug } from "@/data/services";

/**
 * Request-service / contact form helpers.
 *
 * Valid requests are sent to the backend (`POST /api/v1/inquiries`, see lib/api.ts). If that fails,
 * the form offers an e-mail fallback built by buildInquiryMailto.
 */

export interface InquiryValues {
  name: string;
  email: string;
  phone: string;
  /** Slug of the chosen service (see data/services.ts), or "" when the visitor is not sure. */
  service: string;
  message: string;
}

/** Body of the backend's guest inquiry endpoint. */
export interface GuestInquiryPayload {
  name: string;
  email: string;
  phone?: string;
  serviceSlug?: string;
  message: string;
}

export function toGuestInquiryPayload(values: InquiryValues): GuestInquiryPayload {
  const service = getServiceBySlug(values.service);
  return {
    name: values.name.trim(),
    email: values.email.trim(),
    ...(values.phone.trim() ? { phone: values.phone.trim() } : {}),
    ...(service ? { serviceSlug: service.slug } : {}),
    message: values.message.trim(),
  };
}

export type InquiryErrors = Partial<Record<keyof InquiryValues, string>>;

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_PATTERN = /^\+?[0-9][0-9 ()-]{5,19}$/;

export function validateInquiry(values: InquiryValues): InquiryErrors {
  const errors: InquiryErrors = {};
  if (values.name.trim().length < 2) {
    errors.name = "Please enter your name.";
  }
  if (!EMAIL_PATTERN.test(values.email.trim())) {
    errors.email = "Please enter a valid email address.";
  }
  if (values.phone.trim() !== "" && !PHONE_PATTERN.test(values.phone.trim())) {
    errors.phone = "Please enter a valid phone number or leave it empty.";
  }
  if (values.message.trim().length < 10) {
    errors.message = "Please describe your request (at least 10 characters).";
  }
  return errors;
}

export function buildInquiryMailto(to: string, values: InquiryValues): string {
  const serviceTitle = getServiceBySlug(values.service)?.title ?? "";
  const subject = serviceTitle ? `Service request: ${serviceTitle}` : "Website inquiry";
  const lines = [
    `Name: ${values.name.trim()}`,
    `Email: ${values.email.trim()}`,
    values.phone.trim() ? `Phone: ${values.phone.trim()}` : null,
    serviceTitle ? `Service: ${serviceTitle}` : null,
    "",
    values.message.trim(),
  ].filter((line): line is string => line !== null);
  return `mailto:${to}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(lines.join("\n"))}`;
}
