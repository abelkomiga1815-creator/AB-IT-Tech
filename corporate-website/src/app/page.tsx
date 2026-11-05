import Link from "next/link";
import { ServiceCard } from "@/components/ServiceCard";
import { SERVICES } from "@/data/services";
import { SITE_NAME } from "@/config/site";

const AUDIENCES = ["Individuals", "Businesses", "Organizations", "Institutions", "Startups", "Students"];

const REASONS = [
  { title: "One partner for many needs", text: "Software, web, mobile, security, data, design and support from a single team that understands how they fit together." },
  { title: "Built around you", text: "We start by understanding your goals and constraints, then propose a solution that fits your budget and skills." },
  { title: "Secure and dependable", text: "Security, quality and maintainability are part of every project, not an afterthought." },
  { title: "Clear communication", text: "Plain language, honest timelines and regular updates so you always know where things stand." },
];

export default function HomePage() {
  return (
    <>
      <section className="hero">
        <div className="container hero-inner">
          <p className="eyebrow">{SITE_NAME}</p>
          <h1>Technology solutions that help you move forward</h1>
          <p className="lead">
            Software, web and mobile apps, cybersecurity, data analysis, design and IT support for individuals,
            businesses, organizations, institutions and startups.
          </p>
          <div className="hero-actions">
            <Link href="/get-started" className="btn btn-primary">Request a Service</Link>
            <Link href="/services" className="btn btn-outline-light">Explore Services</Link>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="container">
          <h2>Who we work with</h2>
          <ul className="pill-list" aria-label="Customer types">
            {AUDIENCES.map((audience) => (
              <li key={audience}>{audience}</li>
            ))}
          </ul>
        </div>
      </section>

      <section className="section section-alt">
        <div className="container">
          <h2>Our services</h2>
          <p className="section-intro">Seven focused service areas, delivered with the same standard of quality.</p>
          <div className="grid grid-3">
            {SERVICES.map((service) => (
              <ServiceCard key={service.slug} service={service} />
            ))}
          </div>
        </div>
      </section>

      <section className="section">
        <div className="container">
          <h2>Why choose us</h2>
          <div className="grid grid-2">
            {REASONS.map((reason) => (
              <div key={reason.title} className="reason">
                <h3>{reason.title}</h3>
                <p>{reason.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="section section-alt">
        <div className="container split">
          <div>
            <h2>Studying at a university or college?</h2>
            <p>
              Our Student Project Platform is a separate product built for students: manage your project,
              get research assistance, track your progress and prepare for your defense.
            </p>
          </div>
          <Link href="/student-platform" className="btn btn-primary">Discover the Student Platform</Link>
        </div>
      </section>

      <section className="cta">
        <div className="container cta-inner">
          <h2>Ready to start?</h2>
          <p>Tell us what you need and we will get back to you.</p>
          <Link href="/get-started" className="btn btn-light">Request a Service</Link>
        </div>
      </section>
    </>
  );
}
