import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Icon } from "@/components/Icon";
import { PageHero } from "@/components/PageHero";
import { SERVICES, getServiceBySlug } from "@/data/services";

type Params = { slug: string };

export function generateStaticParams(): Params[] {
  return SERVICES.map((service) => ({ slug: service.slug }));
}

export async function generateMetadata({ params }: { params: Promise<Params> }): Promise<Metadata> {
  const { slug } = await params;
  const service = getServiceBySlug(slug);
  return service ? { title: service.title, description: service.summary } : { title: "Service not found" };
}

export default async function ServiceDetailPage({ params }: { params: Promise<Params> }) {
  const { slug } = await params;
  const service = getServiceBySlug(slug);
  if (!service) {
    notFound();
  }

  return (
    <>
      <PageHero title={service.title} intro={service.summary} />
      <section className="section">
        <div className="container split split-top">
          <div className="prose">
            <h2>Overview</h2>
            <p>{service.description}</p>
            <h2>What is included</h2>
            <ul className="check-list">
              {service.features.map((feature) => (
                <li key={feature}>{feature}</li>
              ))}
            </ul>
          </div>
          <aside className="card aside-card" aria-label="Request this service">
            <div className="card-icon"><Icon name={service.icon} /></div>
            <h2>Interested?</h2>
            <p>Tell us about your needs and we will respond with the next steps.</p>
            <Link
              href={`/get-started?service=${encodeURIComponent(service.slug)}`}
              className="btn btn-primary"
            >
              Request this service
            </Link>
          </aside>
        </div>
      </section>
    </>
  );
}
