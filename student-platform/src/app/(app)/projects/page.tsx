"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { Alert } from "@/components/Alert";
import { Loading } from "@/components/Loading";
import { ProgressBar } from "@/components/ProgressBar";
import { StatusBadge } from "@/components/StatusBadge";
import { api, ApiError, fieldErrorsOf, messageOf } from "@/lib/api";
import { PROJECT_STAGES, labelOf } from "@/lib/constants";
import { formatDate } from "@/lib/format";
import type { Project } from "@/lib/types";
import { useLoad } from "@/lib/useLoad";

async function loadProjects(): Promise<{ projects: Project[]; needsProfile: boolean }> {
  try {
    return { projects: await api.get<Project[]>("student/projects"), needsProfile: false };
  } catch (failure) {
    if (failure instanceof ApiError && failure.code === "STUDENT_PROFILE_NOT_FOUND") {
      return { projects: [], needsProfile: true };
    }
    throw failure;
  }
}

export default function ProjectsPage() {
  const { data, error, loading } = useLoad(loadProjects);

  if (loading) {
    return <Loading />;
  }
  if (error || !data) {
    return <Alert>{error ?? "Could not load your projects."}</Alert>;
  }
  if (data.needsProfile) {
    return (
      <>
        <h1>My projects</h1>
        <Alert kind="info">Create your student profile first. <Link href="/profile">Go to my profile</Link></Alert>
      </>
    );
  }

  return (
    <>
      <h1>My projects</h1>
      <NewProjectForm />
      <section className="section-block">
        <h2>All projects</h2>
        {data.projects.length === 0 ? (
          <p className="muted">No projects yet. Add your first one above.</p>
        ) : (
          <ul className="project-list">
            {data.projects.map((project) => (
              <li key={project.id} className="card project-item">
                <div className="row-between">
                  <Link href={`/projects/${project.id}`} className="project-title">{project.title}</Link>
                  <StatusBadge status={project.status} />
                </div>
                <p className="muted">
                  {project.researchArea ? `${project.researchArea} · ` : ""}
                  {project.currentStage ? `Stage: ${labelOf(PROJECT_STAGES, project.currentStage)}` : "No progress recorded yet"}
                  {project.deadline ? ` · Deadline ${formatDate(project.deadline)}` : ""}
                </p>
                <ProgressBar percent={project.progressPercent} label={`Progress of ${project.title}`} />
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}

function NewProjectForm() {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [researchArea, setResearchArea] = useState("");
  const [supervisorName, setSupervisorName] = useState("");
  const [deadline, setDeadline] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setFields({});
    try {
      const created = await api.post<Project>("student/projects", {
        title,
        description: description || null,
        researchArea: researchArea || null,
        supervisorName: supervisorName || null,
        deadline: deadline || null,
      });
      router.push(`/projects/${created.id}`);
    } catch (failure) {
      setError(messageOf(failure));
      setFields(fieldErrorsOf(failure));
      setBusy(false);
    }
  }

  return (
    <section className="card section-block">
      <h2>New project</h2>
      {error && <Alert>{error}</Alert>}
      <form className="form" onSubmit={onSubmit} noValidate>
        <div className="form-row">
          <label htmlFor="title">Project title</label>
          <input id="title" value={title} maxLength={300} required onChange={(e) => setTitle(e.target.value)} />
          {fields.title && <p className="field-error">{fields.title}</p>}
        </div>
        <div className="form-row">
          <label htmlFor="description">Description (optional)</label>
          <textarea id="description" rows={3} maxLength={4000} value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
        <div className="form-grid">
          <div className="form-row">
            <label htmlFor="researchArea">Research area (optional)</label>
            <input id="researchArea" value={researchArea} maxLength={150} onChange={(e) => setResearchArea(e.target.value)} />
          </div>
          <div className="form-row">
            <label htmlFor="supervisorName">Supervisor (optional)</label>
            <input id="supervisorName" value={supervisorName} maxLength={150} onChange={(e) => setSupervisorName(e.target.value)} />
          </div>
          <div className="form-row">
            <label htmlFor="deadline">Deadline (optional)</label>
            <input id="deadline" type="date" value={deadline} onChange={(e) => setDeadline(e.target.value)} />
          </div>
        </div>
        <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? "Creating..." : "Create project"}</button>
      </form>
    </section>
  );
}
