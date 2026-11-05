import type { Metadata } from "next";
import Link from "next/link";
import { PageHero } from "@/components/PageHero";
import { PROJECTS } from "@/data/projects";

export const metadata: Metadata = { title: "Projects" };

export default function ProjectsPage() {
  return (
    <>
      <PageHero title="Projects" intro="A selection of the solutions we have delivered." />
      <section className="section">
        <div className="container">
          {PROJECTS.length === 0 ? (
            <div className="empty-state">
              <h2>Our portfolio is being prepared</h2>
              <p>
                We are putting together case studies that our customers have approved for publication. In the
                meantime, we are happy to discuss relevant experience for your project.
              </p>
              <Link href="/contact" className="btn btn-primary">Talk to us</Link>
            </div>
          ) : (
            <div className="grid grid-3">
              {PROJECTS.map((project) => (
                <article key={project.slug} className="card">
                  <p className="eyebrow dark">{project.category}</p>
                  <h3>{project.title}</h3>
                  <p>{project.summary}</p>
                  <p><strong>Outcome:</strong> {project.outcome}</p>
                </article>
              ))}
            </div>
          )}
        </div>
      </section>
    </>
  );
}
