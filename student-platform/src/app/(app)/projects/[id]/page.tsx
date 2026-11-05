"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { Alert } from "@/components/Alert";
import { Loading } from "@/components/Loading";
import { ProgressBar } from "@/components/ProgressBar";
import { StatusBadge } from "@/components/StatusBadge";
import { api } from "@/lib/api";
import { PROJECT_STAGES, labelOf } from "@/lib/constants";
import type { Project } from "@/lib/types";
import { useLoad } from "@/lib/useLoad";
import { DefensePanel } from "./DefensePanel";
import { DetailsPanel } from "./DetailsPanel";
import { DocumentsPanel } from "./DocumentsPanel";
import { ProgressPanel } from "./ProgressPanel";
import { ResearchPanel } from "./ResearchPanel";

export default function ProjectDetailPage() {
  const params = useParams<{ id: string }>();
  const projectId = params.id;
  const { data: project, error, loading, reload } = useLoad(() => api.get<Project>(`student/projects/${projectId}`));

  if (loading) {
    return <Loading />;
  }
  if (error || !project) {
    return (
      <>
        <Alert>{error ?? "Project not found."}</Alert>
        <p><Link href="/projects">Back to my projects</Link></p>
      </>
    );
  }

  return (
    <>
      <p><Link href="/projects">&larr; My projects</Link></p>
      <div className="row-between">
        <h1>{project.title}</h1>
        <StatusBadge status={project.status} />
      </div>
      <p className="muted">
        {project.currentStage ? `Current stage: ${labelOf(PROJECT_STAGES, project.currentStage)}` : "No progress recorded yet"}
      </p>
      <ProgressBar percent={project.progressPercent} label="Overall progress" />

      <DetailsPanel project={project} onSaved={reload} />
      <ProgressPanel projectId={project.id} onChanged={reload} />
      <ResearchPanel projectId={project.id} />
      <DocumentsPanel projectId={project.id} />
      <DefensePanel projectId={project.id} />
    </>
  );
}
