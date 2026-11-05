import type { Metadata } from "next";
import Link from "next/link";
import { PageHero } from "@/components/PageHero";
import { ServiceCard } from "@/components/ServiceCard";
import { SERVICES } from "@/data/services";

export const metadata: Metadata = {
  title: "Services",
  description: "Software development, web, mobile, cybersecurity, data analysis, UI/UX design and IT support.",
};

export default function ServicesPage() {
  return (
    <>
      <PageHero title="Our services" intro="Everything you need to build, secure and run your technology, from one team." />
      <section className="section">
        <div className="container">
          <div className="grid grid-3">
            {SERVICES.map((service) => (
              <ServiceCard key={service.slug} service={service} />
            ))}
          </div>
          <p className="center">
            Not sure which service you need? <Link href="/contact">Talk to us</Link> and we will help you decide.
          </p>
        </div>
      </section>
    </>
  );
}
