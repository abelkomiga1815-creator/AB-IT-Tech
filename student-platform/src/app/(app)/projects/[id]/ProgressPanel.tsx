"use client";

import { useState, type FormEvent } from "react";
import { Alert } from "@/components/Alert";
import { Loading } from "@/components/Loading";
import { ProgressBar } from "@/components/ProgressBar";
import { api, messageOf } from "@/lib/api";
import { PROJECT_STAGES, labelOf } from "@/lib/constants";
import { formatDate } from "@/lib/format";
import type { ProgressEntry, ProjectStage } from "@/lib/types";
import { useLoad } from "@/lib/useLoad";

export function ProgressPanel({ projectId, onChanged }: { projectId: string; onChanged: () => void }) {
  const history = useLoad(() => api.get<ProgressEntry[]>(`student/projects/${projectId}/progress`));
  const [stage, setStage] = useState<ProjectStage>("TOPIC_SELECTION");
  const [percent, setPercent] = useState(0);
  const [note, setNote] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api.post(`student/projects/${projectId}/progress`, {
        stage,
        percentComplete: percent,
        note: note || null,
      });
      setNote("");
      history.reload();
      onChanged();
    } catch (failure) {
      setError(messageOf(failure));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card section-block">
      <h2>Progress</h2>
      {error && <Alert>{error}</Alert>}
      <form className="form" onSubmit={onSubmit}>
        <div className="form-grid">
          <div className="form-row">
            <label htmlFor="p-stage">Stage</label>
            <select id="p-stage" value={stage} onChange={(e) => setStage(e.target.value as ProjectStage)}>
              {PROJECT_STAGES.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </div>
          <div className="form-row">
            <label htmlFor="p-percent">Overall completion: {percent}%</label>
            <input id="p-percent" type="range" min={0} max={100} step={5} value={percent} onChange={(e) => setPercent(Number(e.target.value))} />
          </div>
        </div>
        <div className="form-row">
          <label htmlFor="p-note">Note (optional)</label>
          <textarea id="p-note" rows={2} maxLength={2000} value={note} onChange={(e) => setNote(e.target.value)} />
        </div>
        <button type="submit" className="btn btn-primary" disabled={busy}>Record progress</button>
      </form>

      <h3>History</h3>
      {history.loading && <Loading />}
      {history.error && <Alert>{history.error}</Alert>}
      {history.data && history.data.length === 0 && <p className="muted">No progress recorded yet.</p>}
      {history.data && history.data.length > 0 && (
        <ul className="timeline">
          {history.data.map((entry) => (
            <li key={entry.id}>
              <div className="row-between">
                <strong>{labelOf(PROJECT_STAGES, entry.stage)}</strong>
                <span className="muted">{formatDate(entry.createdAt)}</span>
              </div>
              <ProgressBar percent={entry.percentComplete} label={`${labelOf(PROJECT_STAGES, entry.stage)} completion`} />
              {entry.note && <p>{entry.note}</p>}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
