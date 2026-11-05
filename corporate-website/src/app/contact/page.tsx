import type { Metadata } from "next";
import { InquiryForm } from "@/components/InquiryForm";
import { PageHero } from "@/components/PageHero";
import { CONTACT } from "@/config/site";

export const metadata: Metadata = { title: "Contact" };

export default function ContactPage() {
  const hasDetails = CONTACT.email || CONTACT.phone || CONTACT.address;
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
            <h2>Contact details</h2>
            {hasDetails ? (
              <ul className="plain-list">
                {CONTACT.email && <li><strong>Email:</strong> <a href={`mailto:${CONTACT.email}`}>{CONTACT.email}</a></li>}
                {CONTACT.phone && <li><strong>Phone:</strong> <a href={`tel:${CONTACT.phone.replace(/\s/g, "")}`}>{CONTACT.phone}</a></li>}
                {CONTACT.address && <li><strong>Address:</strong> {CONTACT.address}</li>}
                {CONTACT.phone && (
                  <li><strong>WhatsApp:</strong> <a href={`https://wa.me/${CONTACT.phone.replace(/\s/g, "").replace(/^\+/, "")}`} target="_blank" rel="noopener">Message us on WhatsApp</a></li>
                )}
              </ul>
            ) : (
              <p>Contact details will be published here soon.</p>
            )}
          </aside>
        </div>
      </section>
    </>
  );
}
