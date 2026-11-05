import Link from "next/link";
import { CONTACT, NAV_LINKS, SITE_NAME } from "@/config/site";
import { SERVICES } from "@/data/services";

export function Footer() {
  return (
    <footer className="site-footer">
      <div className="container footer-grid">
        <div>
          <p className="footer-brand">{SITE_NAME}</p>
          <p className="muted-on-dark">Technology solutions and digital services for everyone who needs reliable technology.</p>
        </div>
        <nav aria-label="Footer">
          <h2 className="footer-title">Company</h2>
          <ul>
            {NAV_LINKS.map((link) => (
              <li key={link.href}>
                <Link href={link.href}>{link.label}</Link>
              </li>
            ))}
          </ul>
        </nav>
        <div>
          <h2 className="footer-title">Services</h2>
          <ul>
            {SERVICES.map((service) => (
              <li key={service.slug}>
                <Link href={`/services/${service.slug}`}>{service.title}</Link>
              </li>
            ))}
          </ul>
        </div>
        <div>
          <h2 className="footer-title">Contact</h2>
          <ul>
            {CONTACT.email && <li><a href={`mailto:${CONTACT.email}`}>{CONTACT.email}</a></li>}
            {CONTACT.phone && <li><a href={`tel:${CONTACT.phone.replace(/\s/g, "")}`}>{CONTACT.phone}</a></li>}
            {CONTACT.address && <li>{CONTACT.address}</li>}
            <li><Link href="/get-started">Request a service</Link></li>
          </ul>
        </div>
      </div>
      <div className="container footer-bottom">
        <p>&copy; {new Date().getFullYear()} {SITE_NAME}. All rights reserved.</p>
      </div>
    </footer>
  );
}
