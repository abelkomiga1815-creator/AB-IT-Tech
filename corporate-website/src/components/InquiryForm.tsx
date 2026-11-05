"use client";

import { useState, type ChangeEvent, type FormEvent } from "react";
import { CONTACT } from "@/config/site";
import { SERVICES } from "@/data/services";
import {
  buildInquiryMailto,
  toGuestInquiryPayload,
  validateInquiry,
  type InquiryErrors,
  type InquiryValues,
} from "@/lib/inquiry";

interface InquiryFormProps {
  /** Slug of a pre-selected service, if the visitor came from a service page. */
  defaultService?: string;
  submitLabel: string;
}

const EMPTY: InquiryValues = { name: "", email: "", phone: "", service: "", message: "" };

type Status = "idle" | "sending" | "sent" | "failed";

/**
 * Request-service / contact form. Valid requests go to the backend;
 * on success an e-mail notification is also sent to the company.
 */
export function InquiryForm({ defaultService = "", submitLabel }: InquiryFormProps) {
  const [values, setValues] = useState<InquiryValues>({ ...EMPTY, service: defaultService });
  const [errors, setErrors] = useState<InquiryErrors>({});
  const [status, setStatus] = useState<Status>("idle");
  const [failure, setFailure] = useState("");

  function update(event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) {
    const { name, value } = event.target;
    setValues((current) => ({ ...current, [name]: value }));
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const found = validateInquiry(values);
    setErrors(found);
    if (Object.keys(found).length > 0) {
      return;
    }
    setStatus("sending");
    setFailure("");
    try {
      await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/v1/inquiries`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(toGuestInquiryPayload(values)),
      });
      setStatus("sent");
      setValues({ ...EMPTY, service: defaultService });
    } catch (error) {
      setFailure(error instanceof Error ? error.message : "We could not send your request.");
      setStatus("failed");
    }
  }

  const field = (name: keyof InquiryValues) => ({
    id: `inquiry-${name}`,
    name,
    value: values[name],
    onChange: update,
    "aria-invalid": errors[name] ? true : undefined,
    "aria-describedby": errors[name] ? `inquiry-${name}-error` : undefined,
  });

  if (status === "sent") {
    return (
      <div className="notice notice-success" role="status">
        <strong>Your service request has been submitted successfully.</strong> We will contact you soon.
        <p><button type="button" className="text-link" onClick={() => setStatus("idle")}>Send another request</button></p>
      </div>
    );
  }

  return (
    <form className="form" onSubmit={onSubmit} noValidate>
      <div className="form-row">
        <label htmlFor="inquiry-name">Full name</label>
        <input type="text" autoComplete="name" required {...field("name")} />
        {errors.name && <p id="inquiry-name-error" className="field-error">{errors.name}</p>}
      </div>
      <div className="form-row">
        <label htmlFor="inquiry-email">Email</label>
        <input type="email" autoComplete="email" required {...field("email")} />
        {errors.email && <p id="inquiry-email-error" className="field-error">{errors.email}</p>}
      </div>
      <div className="form-row">
        <label htmlFor="inquiry-phone">Phone (optional)</label>
        <input type="tel" autoComplete="tel" {...field("phone")} />
        {errors.phone && <p id="inquiry-phone-error" className="field-error">{errors.phone}</p>}
      </div>
      <div className="form-row">
        <label htmlFor="inquiry-service">Service (optional)</label>
        <select {...field("service")}>
          <option value="">Not sure yet</option>
          {SERVICES.map((service) => (
            <option key={service.slug} value={service.slug}>{service.title}</option>
          ))}
        </select>
        {errors.service && <p className="field-error">{errors.service}</p>}
      </div>
      <div className="form-row">
        <label htmlFor="inquiry-message">How can we help?</label>
        <textarea rows={6} required {...field("message")} />
        {errors.message && <p id="inquiry-message-error" className="field-error">{errors.message}</p>}
      </div>
      <button type="submit" className="btn btn-primary" disabled={status === "sending"}>
        {status === "sending" ? "Sending..." : submitLabel}
      </button>
      {status === "failed" && (
        <div className="notice" role="alert">
          {failure}
          {CONTACT.email && (
            <p>
              You can also <a href={buildInquiryMailto(CONTACT.email, values)}>send the same request by email</a>.
            </p>
          )}
        </div>
      )}
    </form>
  );
}