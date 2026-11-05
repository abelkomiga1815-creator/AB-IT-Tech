import Link from "next/link";
import type { Service } from "@/data/services";
import { Icon } from "./Icon";

export function ServiceCard({ service }: { service: Service }) {
  return (
    <article className="card service-card">
      <div className="card-icon">
        <Icon name={service.icon} />
      </div>
      <h3>{service.title}</h3>
      <p>{service.summary}</p>
      <Link href={`/services/${service.slug}`} className="text-link">
        Learn more<span className="sr-only"> about {service.title}</span> &rarr;
      </Link>
    </article>
  );
}
