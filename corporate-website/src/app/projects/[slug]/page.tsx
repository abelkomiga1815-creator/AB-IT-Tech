import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { PageHero } from "@/components/PageHero";
import { PROJECTS } from "@/data/projects";
import { ProjectCard } from "@/components/ProjectCard";

export function generateStaticParams() {
  return PROJECTS.map((project) => ({ slug: project.slug }));
}

export const metadata: Metadata = { title: "Project" };

export default async function ProjectPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const project = PROJECTS.find((p) => p.slug === slug);

  if (!project) {
    notFound();
  }

  const relatedProjects = PROJECTS.filter((p) => p.slug !== project.slug).slice(0, 2);

  return (
    <>
      <PageHero title={project.title} intro={project.category} />
      <section className="section">
        <div className="container split-top">
          <div>
            <img
              src={project.image}
              alt={project.title}
              className="project-detail-image"
              width="800"
              height="450"
              loading="eager"
            />
          </div>
          <div>
            <h2>About this project</h2>
            <p>{project.description}</p>
            <h3>Technologies</h3>
            <div className="project-tech">
              {project.technologies.map((tech) => (
                <span key={tech} className="tech-tag">{tech}</span>
              ))}
            </div>
            <h3>Outcome</h3>
            <p>{project.outcome}</p>
          </div>
        </div>
      </section>
      {relatedProjects.length > 0 && (
        <section className="section section-alt">
          <div className="container">
            <h2>Related Projects</h2>
            <div className="grid grid-3">
              {relatedProjects.map((project) => (
                <ProjectCard key={project.slug} project={project} />
              ))}
            </div>
          </div>
        </section>
      )}
    </>
  );
}
