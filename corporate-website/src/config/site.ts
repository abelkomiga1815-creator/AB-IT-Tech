/**
 * Central site settings. Contact details come from environment variables so no placeholder
 * values are baked into the pages. Replace the logo by putting the official file in /public and
 * changing LOGO_SRC.
 */

export const SITE_NAME = "AB IT Tech Solution";
export const SITE_TAGLINE = "Technology solutions and digital services for people and organizations";
export const SITE_DESCRIPTION =
  "AB IT Tech Solution provides software development, web and mobile apps, cybersecurity, data analysis, UI/UX design and IT support for individuals, businesses, organizations, institutions and startups.";

/** Official logo will be placed in /public by the project owner. */
export const LOGO_SRC = "/logo-placeholder.svg";
export const LOGO_IS_PLACEHOLDER = true;

export const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
export const STUDENT_PLATFORM_URL =
  process.env.NEXT_PUBLIC_STUDENT_PLATFORM_URL ?? "http://localhost:3001";

export const CONTACT = {
  email: process.env.NEXT_PUBLIC_CONTACT_EMAIL ?? "",
  phone: process.env.NEXT_PUBLIC_CONTACT_PHONE ?? "",
  address: process.env.NEXT_PUBLIC_CONTACT_ADDRESS ?? "",
};

export const NAV_LINKS = [
  { href: "/", label: "Home" },
  { href: "/about", label: "About" },
  { href: "/services", label: "Services" },
  { href: "/projects", label: "Projects" },
  { href: "/student-platform", label: "Student Platform" },
  { href: "/contact", label: "Contact" },
] as const;
