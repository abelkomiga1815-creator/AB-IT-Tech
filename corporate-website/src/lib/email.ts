// Server-side email utility for sending service request notifications.
// This module must not be imported from client components; use the API route
// instead, which triggers the email send on the server.

export interface EmailConfig {
  host: string;
  port: number;
  user: string;
  password: string;
}

export const getEmailConfig = (): EmailConfig => {
  return {
    host: process.env.SMTP_HOST ?? "",
    port: Number(process.env.SMTP_PORT) ?? 587,
    user: process.env.SMTP_USER ?? "",
    password: process.env.SMTP_PASSWORD ?? "",
  };
};

export function getServiceTitle(serviceSlug: string): string {
  const serviceMap: Record<string, string> = {
    "software-development": "Software Development",
    "web-development": "Web Development",
    "mobile-development": "Mobile Development",
    "cybersecurity": "Cybersecurity",
    "data-analysis": "Data Analysis",
    "ui-ux-design": "UI/UX Design",
    "it-support": "IT Support",
  };
  return serviceMap[serviceSlug] ?? "Service request";
}

export function buildInquiryEmailBody(values: {
  name: string;
  email: string;
  phone: string;
  service: string;
  message: string;
}): string {
  const serviceTitle = getServiceTitle(values.service);
  const lines = [
    `Name: ${values.name.trim()}`,
    `Email: ${values.email.trim()}`,
    values.phone.trim() ? `Phone: ${values.phone.trim()}` : "",
    `${serviceTitle}: ${values.service}`,
    "",
    values.message.trim(),
  ];
  return lines.filter((line) => line !== "").join("\n");
}