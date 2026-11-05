"use client";

import { useState, type FormEvent } from "react";
import { PROJECT_STATUSES } from "@/lib/constants";
import type { Project, ProjectStatus } from "@/lib/types";

export interface ProjectFormValues {
  title: string;
  description: string | null;
  researchArea: string | null;
  supervisorName: string | null;
  deadline: string | null;
  status?: ProjectStatus;
}

interface ProjectFormProps {
  /** When given, the form edits this project (and shows the status field). */
  project?: Project;
  submitLabel: string;
  busy: boolean;
  onSubmit: (values: ProjectFormValues) => void;
}

const blankToNull = (value: string) => (value.trim() === "" ? null : value.trim());

export function ProjectForm({ project, submitLabel, busy, onSubmit }: ProjectFormProps) {
  const [title, setTitle] = useState(project?.title ?? "");
  const [description, setDescription] = useState(project?.description ?? "");
  const [researchArea, setResearchArea] = useState(project?.researchArea ?? "");
  const [supervisorName, setSupervisorName] = useState(project?.supervisorName ?? "");
  const [deadline, setDeadline] = useState(project?.deadline ?? "");
  const [status, setStatus] = useState<ProjectStatus>(project?.status ?? "PLANNING");

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSubmit({
      title: title.trim(),
      description: blankToNull(description),
      researchArea: blankToNull(researchArea),
      supervisorName: blankToNull(supervisorName),
      deadline: deadline === "" ? null : deadline,
      ...(project ? { status } : {}),
    });
  }

  return (
    <form onSubmit={submit} className="form">
      <div className="form-row">
        <label htmlFor="project-title">Project title</label>
        <input id="project-title" required maxLength={300} value={title} onChange={(e) => setTitle(e.target.value)} />
      </div>
      <div className="form-row">
        <label htmlFor="project-description">Description (optional)</label>
        <textarea id="project-description" rows={4} maxLength={4000} value={description} onChange={(e) => setDescription(e.target.value)} />
      </div>
      <div className="form-grid">
        <div className="form-row">
          <label htmlFor="project-area">Research area (optional)</label>
          <input id="project-area" maxLength={150} value={researchArea} onChange={(e) => setResearchArea(e.target.value)} />
        </div>
        <div className="form-row">
          <label htmlFor="project-supervisor">Supervisor (optional)</label>
          <input id="project-supervisor" maxLength={150} value={supervisorName} onChange={(e) => setSupervisorName(e.target.value)} />
        </div>
        <div className="form-row">
          <label htmlFor="project-deadline">Deadline (optional)</label>
          <input id="project-deadline" type="date" value={deadline} onChange={(e) => setDeadline(e.target.value)} />
        </div>
        {project && (
          <div className="form-row">
            <label htmlFor="project-status">Status</label>
            <select id="project-status" value={status} onChange={(e) => setStatus(e.target.value as ProjectStatus)}>
              {PROJECT_STATUSES.map((value) => (
                <option key={value.value} value={value.value}>{value.label}</option>
              ))}
            </select>
          </div>
        )}
      </div>
      <button type="submit" className="btn btn-primary" disabled={busy}>{submitLabel}</button>
    </form>
  );
}
