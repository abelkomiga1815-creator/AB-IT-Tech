"use client";

import Link from "next/link";
import { Alert } from "@/components/Alert";
import { Loading } from "@/components/Loading";
import { ProgressBar } from "@/components/ProgressBar";
import { StatusBadge } from "@/components/StatusBadge";
import { api, ApiError } from "@/lib/api";
import { PROJECT_STAGES, labelOf } from "@/lib/constants";
import { daysUntil, formatDate } from "@/lib/format";
import type { Project, StudentProfile } from "@/lib/types";
import { useLoad } from "@/lib/useLoad";

async function loadDashboard(): Promise<{ profile: StudentProfile | null; projects: Project[] }> {
  try {
    const [profile, projects] = await Promise.all([
      api.get<StudentProfile>("student/profile"),
      api.get<Project[]>("student/projects"),
    ]);
    return { profile, projects };
  } catch (failure) {
    if (failure instanceof ApiError && failure.code === "STUDENT_PROFILE_NOT_FOUND") {
      return { profile: null, projects: [] };
    }
    throw failure;
  }
}

export default function DashboardPage() {
  const { data, error, loading } = useLoad(loadDashboard);

  if (loading) {
    return <Loading />;
  }
  if (error || !data) {
    return <Alert>{error ?? "Could not load your dashboard."}</Alert>;
  }

  const { profile, projects } = data;
  const active = projects.filter((project) => project.status !== "ARCHIVED");
  const inProgress = active.filter((project) => project.status === "IN_PROGRESS").length;
  const average = active.length === 0 ? 0 : Math.round(active.reduce((sum, p) => sum + p.progressPercent, 0) / active.length);
  const upcoming = active
    .filter((project) => project.deadline && project.status !== "COMPLETED")
    .map((project) => ({ project, days: daysUntil(project.deadline) }))
    .filter((item): item is { project: Project; days: number } => item.days !== null)
    .sort((a, b) => a.days - b.days)[0];

  return (
    <>
      <h1>Dashboard</h1>

      {!profile && (
        <Alert kind="info">
          Complete your student profile to start adding projects. <Link href="/profile">Go to my profile</Link>
        </Alert>
      )}

      {profile && (
        <>
          <div className="grid stats">
            <div className="card stat"><span className="stat-value">{active.length}</span><span className="muted">Projects</span></div>
            <div className="card stat"><span className="stat-value">{inProgress}</span><span className="muted">In progress</span></div>
            <div className="card stat"><span className="stat-value">{average}%</span><span className="muted">Average progress</span></div>
            <div className="card stat">
              <span className="stat-value">{upcoming ? (upcoming.days < 0 ? `${-upcoming.days}d late` : `${upcoming.days}d`) : "-"}</span>
              <span className="muted">{upcoming ? `Next deadline: ${upcoming.project.title}` : "No upcoming deadline"}</span>
            </div>
          </div>

          <section className="section-block">
            <div className="row-between">
              <h2>Recent projects</h2>
              <Link href="/projects" className="btn btn-primary btn-small">Manage projects</Link>
            </div>
            {projects.length === 0 ? (
              <p className="muted">You have no projects yet. <Link href="/projects">Create your first project</Link>.</p>
            ) : (
              <ul className="project-list">
                {projects.slice(0, 5).map((project) => (
                  <li key={project.id} className="card project-item">
                    <div className="row-between">
                      <Link href={`/projects/${project.id}`} className="project-title">{project.title}</Link>
                      <StatusBadge status={project.status} />
                    </div>
                    <p className="muted">
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
      )}

      <section className="section-block">
        <h2>Research assistance</h2>
        <div className="card">
          <p>Open a project and choose <strong>Run research analysis</strong> to find related papers, see a similarity indicator and get evidence-based suggestions about possible research gaps.</p>
        </div>
      </section>
    </>
  );
}
