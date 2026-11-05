import type { MetadataRoute } from "next";
import { SITE_URL } from "@/config/site";
import { SERVICES } from "@/data/services";

export default function sitemap(): MetadataRoute.Sitemap {
  const pages = ["", "/about", "/services", "/projects", "/student-platform", "/contact", "/get-started"];
  return [
    ...pages.map((path) => ({ url: `${SITE_URL}${path}` })),
    ...SERVICES.map((service) => ({ url: `${SITE_URL}/services/${service.slug}` })),
  ];
}
