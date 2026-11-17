import Link from "next/link";
import type { Project } from "@/data/projects";

export function ProjectCard({ project }: { project: Project }) {
  return (
    <article className="card project-card">
      <div className="project-image">
        <img
          src={project.image}
          alt={project.title}
          loading="lazy"
          width="400"
          height="250"
        />
      </div>
      <p className="eyebrow dark">{project.category}</p>
      <h3>{project.title}</h3>
      <p>{project.summary}</p>
      <div className="project-tech">
        {project.technologies.map((tech) => (
          <span key={tech} className="tech-tag">{tech}</span>
        ))}
      </div>
      <Link href={`/projects/${project.slug}`} className="btn btn-primary btn-small">
        View Project
      </Link>
    </article>
  );
}
