import type { Metadata } from "next";
import { InquiryForm } from "@/components/InquiryForm";
import { PageHero } from "@/components/PageHero";
import { CONTACT } from "@/config/site";

export const metadata: Metadata = { title: "Contact" };

export default function ContactPage() {
  const phone = CONTACT.phone?.replace(/\s/g, "") || "+255741802729";
  return (
    <>
      <PageHero title="Contact us" intro="Questions, ideas or a project in mind? We would like to hear from you." />
      <section className="section">
        <div className="container split split-top">
          <div>
            <h2>Send us a message</h2>
            <InquiryForm submitLabel="Send message" />
          </div>
          <aside className="card aside-card" aria-label="Contact details">
            <h2>Contact Us</h2>
            <div className="contact-list">
              <div className="contact-item">
                <div className="contact-head">
                  <svg
                    className="contact-icon"
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
                  <strong className="contact-label">Call Us</strong>
                </div>
                <a href={`tel:${phone}`} className="contact-value" aria-label="Call us">
                  +255 741 802 729
                </a>
                <p className="contact-desc">Speak directly with our team</p>
              </div>
              <div className="contact-item">
                <div className="contact-head">
                  <svg
                    className="contact-icon"
                    viewBox="0 0 24 24"
                    fill="currentColor"
                    aria-hidden="true"
                  >
                    <path d="M18.59 6.18a1.1 1.1 0 0 0-.71-.3.997.997 0 0 0-.45-.12c-.12 0-.23.02-.34.06L14.82 10.59c-.18.18-.16.45.05.63l1.82.82c.18.18.41.41.63.58.18.18.18.45.06.63l-.82 1.82a1.13 1.13 0 0 0-.06.34.997.997 0 0 0-.12.45c0 .12-.02.23-.06.34l1.82.82c.18.18.41.41.58.63.18.18.18.45.06.63l-.82 1.82a1.13 1.13 0 0 0-.34.16.997.997 0 0 0-.45.12c-.12.02-.23.05-.34.05a1.12 1.12 0 0 0-.71-.3 1.12 1.12 0 0 0-.45-.13zM12 4.11a3.03 3.03 0 0 1 1.87.98l1.18 2.09c.39.71.02 1.50-.88 1.89a3.08 3.08 0 0 1-2.86 1.03-3.04 3.04 0 0 1-1.03-2.85.996.996 0 0 0-.17-.31l-1.18-2.09A3.02 3.02 0 0 1 12 4.11zM12 17.89a5.97 5.97 0 0 1-3.02.95-5.98 5.98 0 0 1-3.01-1.01.996.996 0 0 1 1.01-3.02l.93-2.06a5.96 5.96 0 0 1 1.09-3.05c.8 1.01 2.5 1.16 3.64.82 1.13-.33 2.22-.7 3.15-1.18l.56-.86c.09-.13.17-.26.25-.39a.998.998 0 0 0-.01-1.41.997.997 0 0 0-.39-.25c-.97.56-1.82 1.15-2.58 2.06l-.56-.86c-.93.33-1.88.66-2.65 1.18.93-.08 1.82-.15 2.58-.82z" />
                  </svg>
                  <strong className="contact-label">WhatsApp</strong>
                </div>
                <a
                  href={`https://wa.me/${phone.replace(/^\+/, "")}`}
                  className="contact-value"
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label="Message us on WhatsApp"
                >
                  +255 741 802 729
                </a>
                <p className="contact-desc">Chat with us directly</p>
              </div>
              <div className="contact-item">
                <div className="contact-head">
                  <svg
                    className="contact-icon"
                    viewBox="0 0 24 24"
                    fill="currentColor"
                    aria-hidden="true"
                  >
                    <path d="M20 4H4c-1.1 0-1.99 0.9-1.99 2L2 18c0 1.1 0.9 2 2 2h16c1.1 0 2-0.9 2-2V6c0-1.1-0.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z" />
                  </svg>
                  <strong className="contact-label">Email Us</strong>
                </div>
                <a
                  href={`mailto:${CONTACT.email || "abelkomiga1815@gmail.com"}`}
                  className="contact-value"
                  aria-label="Email us"
                >
                  abelkomiga1815@gmail.com
                </a>
                <p className="contact-desc">Send us an inquiry</p>
              </div>
            </div>
          </aside>
        </div>
      </section>
    </>
  );
}
