"use client";

import { useRef, useState, type FormEvent } from "react";
import { Alert } from "@/components/Alert";
import { Loading } from "@/components/Loading";
import { api, messageOf } from "@/lib/api";
import { ALLOWED_UPLOAD_EXTENSIONS, DOCUMENT_TYPES, labelOf } from "@/lib/constants";
import { formatBytes, formatDate } from "@/lib/format";
import type { DocumentType, ProjectDocument } from "@/lib/types";
import { validateUploadFile } from "@/lib/upload";
import { useLoad } from "@/lib/useLoad";

export function DocumentsPanel({ projectId }: { projectId: string }) {
  const base = `student/projects/${projectId}/documents`;
  const documents = useLoad(() => api.get<ProjectDocument[]>(base));
  const fileInput = useRef<HTMLInputElement>(null);
  const [type, setType] = useState<DocumentType>("OTHER");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    const file = fileInput.current?.files?.[0];
    if (!file) {
      setError("Choose a file to upload.");
      return;
    }
    const problem = validateUploadFile(file);
    if (problem) {
      setError(problem);
      return;
    }
    const form = new FormData();
    form.append("file", file);
    form.append("type", type);
    if (description.trim()) {
      form.append("description", description.trim());
    }
    setBusy(true);
    try {
      await api.upload(base, form);
      setDescription("");
      if (fileInput.current) {
        fileInput.current.value = "";
      }
      documents.reload();
    } catch (failure) {
      setError(messageOf(failure));
    } finally {
      setBusy(false);
    }
  }

  async function remove(document: ProjectDocument) {
    if (!window.confirm(`Delete "${document.fileName}"? This cannot be undone.`)) {
      return;
    }
    try {
      await api.delete(`${base}/${document.id}`);
      documents.reload();
    } catch (failure) {
      setError(messageOf(failure));
    }
  }

  return (
    <section className="card section-block">
      <h2>Documents</h2>
      {error && <Alert>{error}</Alert>}
      <form className="form" onSubmit={onSubmit}>
        <div className="form-grid">
          <div className="form-row">
            <label htmlFor="doc-file">File</label>
            <input id="doc-file" type="file" ref={fileInput} accept={ALLOWED_UPLOAD_EXTENSIONS.map((e) => `.${e}`).join(",")} />
            <p className="hint">Up to 10 MB. Allowed: {ALLOWED_UPLOAD_EXTENSIONS.join(", ")}.</p>
          </div>
          <div className="form-row">
            <label htmlFor="doc-type">Type</label>
            <select id="doc-type" value={type} onChange={(e) => setType(e.target.value as DocumentType)}>
              {DOCUMENT_TYPES.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </div>
        </div>
        <div className="form-row">
          <label htmlFor="doc-description">Description (optional)</label>
          <input id="doc-description" value={description} maxLength={500} onChange={(e) => setDescription(e.target.value)} />
        </div>
        <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? "Uploading..." : "Upload document"}</button>
      </form>

      <h3>Uploaded documents</h3>
      {documents.loading && <Loading />}
      {documents.error && <Alert>{documents.error}</Alert>}
      {documents.data && documents.data.length === 0 && <p className="muted">No documents uploaded yet.</p>}
      {documents.data && documents.data.length > 0 && (
        <ul className="doc-list">
          {documents.data.map((document) => (
            <li key={document.id}>
              <div>
                <strong>{document.fileName}</strong>
                <p className="muted">
                  {labelOf(DOCUMENT_TYPES, document.type)} · {formatBytes(document.sizeBytes)} · {formatDate(document.createdAt)}
                </p>
                {document.description && <p>{document.description}</p>}
              </div>
              <div className="doc-actions">
                <a className="btn btn-outline btn-small" href={`/api/backend/${base}/${document.id}/download`}>Download</a>
                <button type="button" className="btn btn-danger btn-small" onClick={() => remove(document)}>Delete</button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
