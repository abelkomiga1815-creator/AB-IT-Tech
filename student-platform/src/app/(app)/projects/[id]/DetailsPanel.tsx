"use client";

import { useState, type FormEvent } from "react";
import { Alert } from "@/components/Alert";
import { api, fieldErrorsOf, messageOf } from "@/lib/api";
import { PROJECT_STATUSES } from "@/lib/constants";
import type { Project, ProjectStatus } from "@/lib/types";

export function DetailsPanel({ project, onSaved }: { project: Project; onSaved: () => void }) {
  const [title, setTitle] = useState(project.title);
  const [description, setDescription] = useState(project.description ?? "");
  const [researchArea, setResearchArea] = useState(project.researchArea ?? "");
  const [supervisorName, setSupervisorName] = useState(project.supervisorName ?? "");
  const [deadline, setDeadline] = useState(project.deadline ?? "");
  const [status, setStatus] = useState<ProjectStatus>(project.status);
  const [message, setMessage] = useState<{ kind: "success" | "error"; text: string } | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setMessage(null);
    setFields({});
    try {
      await api.put<Project>(`student/projects/${project.id}`, {
        title,
        description: description || null,
        researchArea: researchArea || null,
        supervisorName: supervisorName || null,
        deadline: deadline || null,
        status,
      });
      setMessage({ kind: "success", text: "Project details saved." });
      onSaved();
    } catch (failure) {
      setMessage({ kind: "error", text: messageOf(failure) });
      setFields(fieldErrorsOf(failure));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card section-block">
      <h2>Details</h2>
      {message && <Alert kind={message.kind}>{message.text}</Alert>}
      <form className="form" onSubmit={onSubmit} noValidate>
        <div className="form-row">
          <label htmlFor="d-title">Title</label>
          <input id="d-title" value={title} maxLength={300} required onChange={(e) => setTitle(e.target.value)} />
          {fields.title && <p className="field-error">{fields.title}</p>}
        </div>
        <div className="form-row">
          <label htmlFor="d-description">Description</label>
          <textarea id="d-description" rows={4} maxLength={4000} value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
        <div className="form-grid">
          <div className="form-row">
            <label htmlFor="d-area">Research area</label>
            <input id="d-area" value={researchArea} maxLength={150} onChange={(e) => setResearchArea(e.target.value)} />
          </div>
          <div className="form-row">
            <label htmlFor="d-supervisor">Supervisor</label>
            <input id="d-supervisor" value={supervisorName} maxLength={150} onChange={(e) => setSupervisorName(e.target.value)} />
          </div>
          <div className="form-row">
            <label htmlFor="d-deadline">Deadline</label>
            <input id="d-deadline" type="date" value={deadline} onChange={(e) => setDeadline(e.target.value)} />
          </div>
          <div className="form-row">
            <label htmlFor="d-status">Status</label>
            <select id="d-status" value={status} onChange={(e) => setStatus(e.target.value as ProjectStatus)}>
              {PROJECT_STATUSES.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </div>
        </div>
        <button type="submit" className="btn btn-primary" disabled={busy}>Save details</button>
      </form>
    </section>
  );
}
