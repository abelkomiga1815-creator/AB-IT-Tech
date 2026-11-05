import type { Metadata } from "next";
import { InquiryForm } from "@/components/InquiryForm";
import { PageHero } from "@/components/PageHero";
import { getServiceBySlug } from "@/data/services";

export const metadata: Metadata = { title: "Request a Service" };

const STEPS = [
  { title: "Tell us what you need", text: "Choose a service and describe your request." },
  { title: "We get in touch", text: "We review your request and contact you to clarify the details." },
  { title: "We propose a solution", text: "You receive a clear proposal with scope and next steps." },
];

export default async function GetStartedPage({
  searchParams,
}: {
  searchParams: Promise<{ service?: string | string[] }>;
}) {
  const { service } = await searchParams;
  const slug = Array.isArray(service) ? service[0] : service;
  const selected = slug ? getServiceBySlug(slug) : undefined;

  return (
    <>
      <PageHero title="Request a service" intro="Three simple steps from your idea to a solution." />
      <section className="section">
        <div className="container split split-top">
          <div>
            <h2>Your request</h2>
            <InquiryForm defaultService={selected?.slug ?? ""} submitLabel="Send request" />
          </div>
          <aside className="card aside-card" aria-label="How it works">
            <h2>How it works</h2>
            <ol className="steps">
              {STEPS.map((step) => (
                <li key={step.title}>
                  <strong>{step.title}</strong>
                  <span>{step.text}</span>
                </li>
              ))}
            </ol>
          </aside>
        </div>
      </section>
    </>
  );
}
