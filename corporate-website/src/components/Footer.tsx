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
            <li>
              <div className="contact-row">
                <svg
                  className="contact-icon w-4 h-4"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                >
                  <path d="M8 3h8a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1ZM11 18h2" />
                </svg>
                <div>
                  <strong className="contact-label">Call Us</strong>
                  <a
                    href={`tel:${CONTACT.phone?.replace(/\s/g, "") ?? "255741802729"}`
                    }
                    className="contact-link"
                    aria-label="Call us"
                  >
                    +255 741 802 729
                  </a>
                </div>
              </div>
            </li>
            <li>
              <div className="contact-row">
                <svg
                  className="contact-icon w-4 h-4 text-green-600"
                  viewBox="0 0 24 24"
                  fill="currentColor"
                  aria-hidden="true"
                >
                  <path d="M18.59 6.18a1.1 1.1 0 0 0-.71-.3.997.997 0 0 0-.45-.12c-.12 0-.23.02-.34.06L14.82 10.59c-.18.18-.16.45.05.63l1.82.82c.18.18.41.41.63.58.18.18.18.45.06.63l-.82 1.82a1.13 1.13 0 0 0-.06.34.997.997 0 0 0-.12.45c0 .12-.02.23-.06.34l1.82.82c.18.18.41.41.58.63.18.18.18.45.06.63l-.82 1.82a1.13 1.13 0 0 0-.34.16.997.997 0 0 0-.45.12c-.12.02-.23.05-.34.05a1.12 1.12 0 0 0-.71-.3 1.12 1.12 0 0 0-.45-.13zM12 4.11a3.03 3.03 0 0 1 1.87.98l1.18 2.09c.39.71.02 1.50-.88 1.89a3.08 3.08 0 0 1-2.86 1.03-3.04 3.04 0 0 1-1.03-2.85.996.996 0 0 0-.17-.31l-1.18-2.09A3.02 3.02 0 0 1 12 4.11zM12 17.89a5.97 5.97 0 0 1-3.02.95-5.98 5.98 0 0 1-3.01-1.01.996.996 0 0 1 1.01-3.02l.93-2.06a5.96 5.96 0 0 1 1.09-3.05c.8 1.01 2.5 1.16 3.64.82 1.13-.33 2.22-.7 3.15-1.18l.56-.86c.09-.13.17-.26.25-.39a.998.998 0 0 0-.01-1.41.997.997 0 0 0-.39-.25c-.97.56-1.82 1.15-2.58 2.06l-.56-.86c-.93.33-1.88.66-2.65 1.18.93-.08 1.82-.15 2.58-.82z" />
                </svg>
                <div>
                  <strong className="contact-label">Chat on WhatsApp</strong>
                  <a
                    href={`https://wa.me/${CONTACT.phone?.replace(/\s/g, "") ?? "255741802729"}`
                    }
                    className="contact-link"
                    target="_blank"
                    rel="noopener noreferrer"
                    aria-label="Message us on WhatsApp"
                  >
                    +255 741 802 729
                  </a>
                </div>
              </div>
            </li>
            {CONTACT.address && (
              <li>
                <a href={`mailto:${CONTACT.email}`}>{CONTACT.address}</a>
              </li>
            )}
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