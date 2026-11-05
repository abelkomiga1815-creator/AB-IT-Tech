"use client";

import { useMemo, useState } from "react";
import { Alert } from "@/components/Alert";
import { Loading } from "@/components/Loading";
import { ReportView } from "@/components/ReportView";
import { api, messageOf } from "@/lib/api";
import { formatDate } from "@/lib/format";
import { SIMILARITY_LABELS } from "@/lib/research";
import { safeHttpUrl } from "@/lib/safe-url";
import type { AnalysisDetail, AnalysisSummary, RelatedPaper, SavedSource } from "@/lib/types";
import { useLoad } from "@/lib/useLoad";

export function ResearchPanel({ projectId }: { projectId: string }) {
  const base = `student/projects/${projectId}/research`;
  const analyses = useLoad(() => api.get<AnalysisSummary[]>(`${base}/analyses`));
  const sources = useLoad(() => api.get<SavedSource[]>(`${base}/sources`));
  const [selected, setSelected] = useState<AnalysisDetail | null>(null);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const savedTitles = useMemo(
    () => new Set((sources.data ?? []).map((source) => source.title.toLowerCase())),
    [sources.data],
  );

  async function run() {
    setRunning(true);
    setError(null);
    try {
      const created = await api.post<AnalysisDetail>(`${base}/analyses`);
      setSelected(created);
      analyses.reload();
    } catch (failure) {
      setError(messageOf(failure));
    } finally {
      setRunning(false);
    }
  }

  async function open(id: string) {
    setError(null);
    try {
      setSelected(await api.get<AnalysisDetail>(`${base}/analyses/${id}`));
    } catch (failure) {
      setError(messageOf(failure));
    }
  }

  async function save(paper: RelatedPaper) {
    setError(null);
    try {
      await api.post<SavedSource>(`${base}/sources`, {
        title: paper.title,
        authors: paper.authors.join(", ") || null,
        year: paper.year,
        venue: paper.venue,
        doi: paper.doi,
        url: safeHttpUrl(paper.url),
        provider: paper.sources[0] ?? null,
      });
      sources.reload();
    } catch (failure) {
      setError(messageOf(failure));
    }
  }

  async function removeSource(source: SavedSource) {
    try {
      await api.delete(`${base}/sources/${source.id}`);
      sources.reload();
    } catch (failure) {
      setError(messageOf(failure));
    }
  }

  return (
    <section className="card section-block">
      <h2>Research assistance</h2>
      <p className="muted">
        Finds related papers in OpenAlex, Crossref and Semantic Scholar and gives you an indicator of how close
        they are to your project. It uses your project title and description. It is a starting point for your own
        reading, not a verdict on your project.
      </p>
      {error && <Alert>{error}</Alert>}
      <button type="button" className="btn btn-primary" disabled={running} onClick={run}>
        {running ? "Analysing... this can take up to a minute" : "Run research analysis"}
      </button>

      {selected && (
        <div className="section-block">
          <h3>Analysis from {formatDate(selected.createdAt)}</h3>
          <ReportView report={selected.report} onSave={save} savedTitles={savedTitles} />
        </div>
      )}

      <h3>Previous analyses</h3>
      {analyses.loading && <Loading />}
      {analyses.error && <Alert>{analyses.error}</Alert>}
      {analyses.data && analyses.data.length === 0 && <p className="muted">No analyses yet.</p>}
      {analyses.data && analyses.data.length > 0 && (
        <ul className="doc-list">
          {analyses.data.map((analysis) => (
            <li key={analysis.id}>
              <div>
                <strong>{formatDate(analysis.createdAt)}</strong>
                <p className="muted">
                  {analysis.papersFound} paper(s) · similarity: {SIMILARITY_LABELS[analysis.similarityLevel]}
                </p>
              </div>
              <button type="button" className="btn btn-outline btn-small" onClick={() => open(analysis.id)}>View</button>
            </li>
          ))}
        </ul>
      )}

      <h3>My reading list</h3>
      {sources.loading && <Loading />}
      {sources.data && sources.data.length === 0 && <p className="muted">Save papers from an analysis to build your reading list.</p>}
      {sources.data && sources.data.length > 0 && (
        <ul className="doc-list">
          {sources.data.map((source) => {
            const href = safeHttpUrl(source.url);
            return (
              <li key={source.id}>
                <div>
                  <strong>{href ? <a href={href} target="_blank" rel="noopener noreferrer">{source.title}</a> : source.title}</strong>
                  <p className="muted">{[source.year, source.venue, source.authors].filter(Boolean).join(" · ")}</p>
                </div>
                <button type="button" className="btn btn-danger btn-small" onClick={() => removeSource(source)}>Remove</button>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
